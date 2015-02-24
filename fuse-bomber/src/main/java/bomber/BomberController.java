package bomber;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@RestController
public class BomberController {
    @Value("classpath:/zevel.txt")
    String zevel;

    final AtomicBoolean mustStop = new AtomicBoolean(false);
    final AtomicLong lastPrint = new AtomicLong(0);
    final AtomicLong allCount = new AtomicLong(0);
    final AtomicLong allLatency = new AtomicLong(0);
    ExecutorService tp = Executors.newFixedThreadPool(200);

    @RequestMapping(value = "/attack/{ip}/{threads}", method= RequestMethod.POST)
    public String startBombing(final @PathVariable String ip, @PathVariable int threads) {
        mustStop.set(false);

        for(int i=0; i<threads; i++) {
            tp.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
                    while (!mustStop.get()) {
                        try {
                            Future<Response> f = asyncHttpClient.preparePost("http://" + ip + "/checkin")
                                    .setHeader("Content-Type", "application/json")
                                    .setBody(generateBody()).execute();
                            long start = System.currentTimeMillis();
                            Response r = f.get();
                            long stop = System.currentTimeMillis();
                            long wasLastPrint = lastPrint.get();
                            long latency = stop - start;
                            allCount.incrementAndGet();
                            allLatency.addAndGet(latency);
                            if (stop - lastPrint.get() > 5000) {
                                if (lastPrint.compareAndSet(wasLastPrint, stop)) {
                                    long oldCount = allCount.get();
                                    long oldLatency = allLatency.get();
                                    allCount.set(0);
                                    allLatency.set(0);
                                    if (oldCount > 0)
                                        System.out.println("*** latency: " + (oldLatency / oldCount) + "ms out of requests:" + oldCount);
                                }
                            }
                        } catch (Exception e) {
                            e.getLocalizedMessage();
                            //e.printStackTrace();
                        }
                    }
                    return null;
                }
            });
        }
        return "Started";
    }

    private String generateNum() {
        double rnd1 = Math.random();
        double rnd2 = Math.random();
        if (rnd1 < .01)
            return zevel;
        if (rnd1 < .05)
            return "ororo.&39a.^393#$@023";
        int gr = (int)(rnd2 * 10)*8;
        return Double.toString(gr + rnd1);
    }

    private String generateBody() {
        UUID uuid = UUID.randomUUID();
        return "{\"userId\": \""+uuid+"\", \"latitude\":"+generateNum()+", \"longitude\":"+generateNum()+"}";
    }

    @RequestMapping(value = "/attack", method = RequestMethod.DELETE)
    public String stopBombing() {
        mustStop.set(true);
        return "Stopped";
    }
}
