package org.anhonesteffort.usrp;

import org.anhonesteffort.dsp.ChannelSpec;
import org.anhonesteffort.dsp.sample.DynamicSink;
import org.anhonesteffort.dsp.sample.Samples;
import org.anhonesteffort.dsp.sample.TunableSamplesSource;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class UsrpTest {

  private static class SimpleSamplesSink implements DynamicSink<Samples> {

    private long   sampleRate  = -1l;
    private double frequency   = -1d;
    private long   sampleCount = 0;

    @Override
    public void onSourceStateChange(Long aLong, Double aDouble) {
      this.sampleRate = aLong;
      this.frequency  = aDouble;
    }

    @Override
    public void consume(Samples samples) {
      sampleCount += samples.getSamples().limit();
    }

    public long getSampleRate() {
      return sampleRate;
    }

    public double getFrequency() {
      return frequency;
    }

    public long getSampleCount() {
      return sampleCount;
    }
  }

  @Test
  public void testSetSampleRate() throws Exception {
    final UsrpProvider         PROVIDER = new UsrpProvider();
    final TunableSamplesSource USRP     = PROVIDER.get().get();
    final SimpleSamplesSink    SINK     = new SimpleSamplesSink();

    USRP.addSink(SINK);

    final ChannelSpec CHANNEL = USRP.getTunedChannel();
    assert SINK.getSampleRate() == CHANNEL.getSampleRate();
    assert SINK.getFrequency()  == CHANNEL.getCenterFrequency();

    final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    final Future          FUTURE   = EXECUTOR.submit(USRP);

    Thread.sleep(1000);

    final long MIN_SAMPLES_PRODUCED = (long) (CHANNEL.getSampleRate() * 0.80d);
    assert SINK.getSampleCount() > MIN_SAMPLES_PRODUCED;

    USRP.removeSink(SINK);
    FUTURE.cancel(true);
  }

}
