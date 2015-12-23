/*
 * Copyright (C) 2015 An Honest Effort LLC, fuck the police.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.anhonesteffort.usrp;

import org.anhonesteffort.dsp.sample.Samples;
import org.anhonesteffort.dsp.sample.SamplesSourceBrokenException;
import org.anhonesteffort.dsp.sample.SamplesSourceException;
import org.anhonesteffort.dsp.sample.TunableSamplesSource;
import org.anhonesteffort.uhd.RxStreamer;
import org.anhonesteffort.uhd.StreamArgs;
import org.anhonesteffort.uhd.types.DeviceAddress;
import org.anhonesteffort.uhd.types.RxMetadata;
import org.anhonesteffort.uhd.types.StreamCommand;
import org.anhonesteffort.uhd.types.TuneRequest;
import org.anhonesteffort.uhd.usrp.MultiUsrp;
import org.anhonesteffort.uhd.usrp.SubDeviceSpec;
import org.anhonesteffort.uhd.util.ComplexFloatVector;
import org.anhonesteffort.uhd.util.StringVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Usrp extends TunableSamplesSource {

  private final DeviceAddress address;
  private final MultiUsrp     multiUsrp;
  private final UsrpConfig    config;
  private final long          rxBufferSize;
  private final double        clockRate;

  protected Usrp(DeviceAddress address, MultiUsrp multiUsrp, UsrpConfig config)
      throws SamplesSourceException
  {
    super(config.getMaxSampleRate(),
          config.getMinRxFrequency(),
          config.getMaxRxFrequency());

    this.address      = address;
    this.multiUsrp    = multiUsrp;
    this.config       = config;
    this.rxBufferSize = config.getRxBufferSize();
    this.clockRate    = multiUsrp.get_master_clock_rate(0);

    applyConfig();
  }

  private void applyConfig() throws SamplesSourceException {

    try {

      multiUsrp.set_rx_subdev_spec(new SubDeviceSpec(config.getSubDevice()), MultiUsrp.ALL_MBOARDS);
      multiUsrp.set_clock_source(config.getClockSource(), MultiUsrp.ALL_MBOARDS);
      multiUsrp.set_rx_antenna(config.getRxAntenna(), 0);

      if (config.getGainName1() != null)
        multiUsrp.set_rx_gain(config.getGainVal1(), config.getGainName1(), 0);
      if (config.getGainName2() != null)
        multiUsrp.set_rx_gain(config.getGainVal2(), config.getGainName2(), 0);

      setSampleRate(UsrpConfig.DEFAULT_SAMPLE_RATE);
      setFrequency(config.getMinRxFrequency());

      StringVector rxSensorNames = multiUsrp.get_rx_sensor_names(0);
      for (long i = 0; i < rxSensorNames.size(); i++) {
        if (rxSensorNames.get(i).equals("lo_locked")) {
          if (!multiUsrp.get_rx_sensor("lo_locked", 0).to_bool())
            throw new SamplesSourceException("ettus says that lo_locked must be true");
          break;
        }
      }

    } catch (RuntimeException e) {
      throw new SamplesSourceException("unable to configure uhd device " + address.to_string(), e);
    }
  }

  @Override
  protected Long setSampleRate(Long minSampleRate) throws SamplesSourceException {
    int  decimation        = (int) Math.ceil(clockRate / minSampleRate);
    long allowedSampleRate = (long) (clockRate / decimation);

    if (allowedSampleRate < minSampleRate) {
      decimation        = ((decimation & 1) == 1) ? decimation - 1 : decimation - 2;
      allowedSampleRate = (long) (clockRate / decimation);
    }

    try {

      multiUsrp.set_rx_rate(allowedSampleRate);
      this.sampleRate = (long) multiUsrp.get_rx_rate(0);
      return this.sampleRate;

    } catch (RuntimeException e) {
      throw new SamplesSourceException("error setting usrp sample rate " + address.to_string(), e);
    }
  }

  @Override
  protected Double setFrequency(Double frequency) throws SamplesSourceException {
    try {

      multiUsrp.set_rx_freq(
          new TuneRequest(frequency + config.getFrequencyCorrection()), 0
      );

      this.frequency = (multiUsrp.get_rx_freq(0) - config.getFrequencyCorrection());
      return this.frequency;

    } catch (RuntimeException e) {
      throw new SamplesSourceException("error setting usrp frequency " + address.to_string(), e);
    }
  }

  @Override
  protected Runnable newProducer() {
    return new SamplesProducer();
  }

  private class SamplesProducer implements Runnable {

    private final Logger     log = LoggerFactory.getLogger(SamplesProducer.class);
    private final RxStreamer rxStreamer;

    public SamplesProducer() {
      rxStreamer = multiUsrp.getRxStream(new StreamArgs("fc32", "sc16"));
    }

    private void start() {
      multiUsrp.issue_stream_cmd(
          new StreamCommand(StreamCommand.START_CONTINUOUS),
          MultiUsrp.ALL_CHANS
      );
    }

    private void drainHardwareBuffer() {
      long               drainTimeout  = System.currentTimeMillis() + 5000l;
      RxMetadata         rxMetadata    = new RxMetadata();
      ComplexFloatVector samplesVector = new ComplexFloatVector(rxBufferSize);

      while (rxMetadata.error_code()    != RxMetadata.ERROR_TIMEOUT &&
             System.currentTimeMillis() <= drainTimeout)
      {
        rxStreamer.recv(samplesVector.front(),
                        samplesVector.size(),
                        rxMetadata, 0.1, false);
      }

      if (rxMetadata.error_code() != RxMetadata.ERROR_TIMEOUT)
        throw new SamplesSourceBrokenException("failed to drain usrp hardware receive buffer");
    }

    private void stop() {
      try {

        multiUsrp.issue_stream_cmd(
            new StreamCommand(StreamCommand.STOP_CONTINUOUS),
            MultiUsrp.ALL_CHANS
        );
        drainHardwareBuffer();

      } catch (RuntimeException e) {
        throw new SamplesSourceBrokenException("unable to stop usrp receive stream", e);
      }
    }

    @Override
    public void run() {
      ComplexFloatVector samplesVector = new ComplexFloatVector(rxBufferSize);
      RxMetadata         rxMetadata    = new RxMetadata();

      try {

        start();
        while (!Thread.interrupted()) {
          rxStreamer.recv(samplesVector.front(),
                          samplesVector.size(),
                          rxMetadata, 0.1, false);

          if (rxMetadata.error_code() == RxMetadata.ERROR_OVERFLOW) {
            log.warn("usrp hardware receive buffer has overflowed");
          } else if (rxMetadata.error_code() != RxMetadata.ERROR_NONE) {
            throw new SamplesSourceBrokenException("usrp receive returned error " + rxMetadata.error_code());
          } else {
            broadcast(new Samples(samplesVector.toFloatBuffer()));
          }

          samplesVector = new ComplexFloatVector(rxBufferSize);
        }
        stop();

      } catch (RuntimeException e) {
        throw new SamplesSourceBrokenException("error receiving samples from usrp", e);
      }
    }
  }
}
