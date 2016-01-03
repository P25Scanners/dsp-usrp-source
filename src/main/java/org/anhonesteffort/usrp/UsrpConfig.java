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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class UsrpConfig {

  public static final long DEFAULT_SAMPLE_RATE = 100000;

  private final String clockSource;
  private final String subDevice;
  private final String rxAntenna;
  private final long   maxSampleRate;
  private final double minRxFrequency;
  private final double maxRxFrequency;
  private final double frequencyCorrection;
  private final String gainName1;
  private final String gainName2;
  private final double gainVal1;
  private final double gainVal2;
  private final long   rxBufferSize;
  private final long   rxDrainTimeoutMs;

  public UsrpConfig() throws IOException {
    Properties properties = new Properties();
    properties.load(new FileInputStream("usrp.properties"));

    clockSource         = properties.getProperty("clock_source");
    subDevice           = properties.getProperty("sub_device");
    rxAntenna           = properties.getProperty("rx_antenna");
    maxSampleRate       = Long.parseLong(properties.getProperty("max_sample_rate"));
    minRxFrequency      = Double.parseDouble(properties.getProperty("min_rx_freq"));
    maxRxFrequency      = Double.parseDouble(properties.getProperty("max_rx_freq"));
    frequencyCorrection = Double.parseDouble(properties.getProperty("frequency_correction"));
    gainName1           = properties.getProperty("gain_name1");
    gainName2           = properties.getProperty("gain_name2");
    gainVal1            = Double.parseDouble(properties.getProperty("gain_val1"));
    gainVal2            = Double.parseDouble(properties.getProperty("gain_val2"));
    rxBufferSize        = Long.parseLong(properties.getProperty("rx_buff_size"));
    rxDrainTimeoutMs    = Long.parseLong(properties.getProperty("rx_drain_timeout_ms"));
  }

  public String getClockSource() {
    return clockSource;
  }

  public String getSubDevice() {
    return subDevice;
  }

  public String getRxAntenna() {
    return rxAntenna;
  }

  public long getMaxSampleRate() {
    return maxSampleRate;
  }

  public double getMinRxFrequency() {
    return minRxFrequency;
  }

  public double getMaxRxFrequency() {
    return maxRxFrequency;
  }

  public double getFrequencyCorrection() {
    return frequencyCorrection;
  }

  public String getGainName1() {
    return gainName1;
  }

  public String getGainName2() {
    return gainName2;
  }

  public double getGainVal1() {
    return gainVal1;
  }

  public double getGainVal2() {
    return gainVal2;
  }

  public long getRxBufferSize() {
    return rxBufferSize;
  }

  public long getRxDrainTimeoutMs() {
    return rxDrainTimeoutMs;
  }

}
