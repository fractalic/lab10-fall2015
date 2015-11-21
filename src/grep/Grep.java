package grep;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/** Search web pages for lines matching a pattern. */
public class Grep {

    public static void main(String[] args) throws Exception {

        // substring to search for
        String substring = "CPEN 221";

        // URLs to search
        String[] urls = new String[] { "http://cpen221.ece.ubc.ca/",
                "https://github.com/CPEN-221/mp5-fall2015",
                "https://github.com/CPEN-221/lab10-fall2015", };

        // list for accumulating matching lines
        List<Line> matches = Collections
                .synchronizedList(new ArrayList<Line>());

        // queue for sending lines from producers to consumers
        BlockingQueue<Line> queue = new LinkedBlockingQueue<Line>();

        Thread[] producers = new Thread[urls.length]; // one producer per URL
        Thread[] consumers = new Thread[100];

        for (int ii = 0; ii < consumers.length; ii++) { // start Consumers
            Thread consumer = consumers[ii] = new Thread(
                    new Consumer(queue, substring, matches));
            consumer.start();
        }

        for (int ii = 0; ii < urls.length; ii++) { // start Producers
            Thread producer = producers[ii] = new Thread(
                    new Producer(urls[ii], queue));
            producer.start();
        }

        for (Thread producer : producers) { // wait for Producers to stop
            producer.join();
        }

        for (int ii = 0; ii < consumers.length; ii++) {
            queue.add(new Text("terminate", 0, "terminate"));
        }

        for (Thread consumer : consumers) { // wait for Consumers to stop
            consumer.join();
        }

        for (Line match : matches) {
            System.out.println(match);
        }
        System.out.println(matches.size() + " lines matched");
    }
}

class Producer implements Runnable {

    private final String              url;
    private final BlockingQueue<Line> queue;

    Producer(String url, BlockingQueue<Line> queue) {
        this.url = url;
        this.queue = queue;
    }

    public void run() {
        int lineNumber = 1;
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(new URL(url).openStream()));
            String line;
            while ((line = in.readLine()) != null) {
                Line numberedLine = new Text(url, lineNumber, line);

                try {
                    queue.put(numberedLine);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                lineNumber++;

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

class Consumer implements Runnable {

    private final BlockingQueue<Line> queue;
    private final List<Line>          matches;
    private final String              substring;
    private final int                 consumerID;
    static private int                consumerCounter = 1;

    Consumer(BlockingQueue<Line> queue, String substring, List<Line> matches) {
        this.queue = queue;
        this.substring = substring;
        this.matches = matches;
        consumerID = consumerCounter;
        consumerCounter++;
    }

    public void run() {
        Line numberedLine;
        while (true) {
            try {
                numberedLine = queue.take();
                if (numberedLine.lineNumber() == 0) {
                    break;
                }
                if (numberedLine.text().contains(substring)) {
                    matches.add(numberedLine);
                    //System.out.println(consumerID + ":" + numberedLine);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

}

interface Line {

    /** @return the filename. */
    public String filename();

    /** @return the line number. */
    public int lineNumber();

    /** @return the text on the line. */
    public String text();
}

class Text implements Line {

    private final String filename;
    private final int    lineNumber;
    private final String text;

    public Text(String filename, int lineNumber, String text) {
        this.filename = filename;
        this.lineNumber = lineNumber;
        this.text = text;
    }

    public String filename() {
        return filename;
    }

    public int lineNumber() {
        return lineNumber;
    }

    public String text() {
        return text;
    }

    @Override
    public String toString() {
        return filename + ":" + lineNumber + ":" + text;
    }
}
