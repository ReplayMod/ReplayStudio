package de.johni0702.replaystudio;

import de.johni0702.replaystudio.api.Replay;
import de.johni0702.replaystudio.api.ReplayPart;
import de.johni0702.replaystudio.api.Studio;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Launcher {
    public static void main(String[] args) throws IOException {
        File folder = new File("/home/johni0702/tmp/");
        Studio studio = new ReplayStudio();
        long start = System.currentTimeMillis();

        Replay replay = studio.createReplay(new FileInputStream(new File(folder, "replay.mcpr")));

        long end = System.currentTimeMillis();
        System.out.println("Finished importing after " + (end - start));
        start = end;

        System.out.println("Duration: " + replay.length());
        System.out.println("Packets: " + replay.size());

        ReplayPart before = replay.viewOf(0, (2 * 60 + 13) * 1000);
        ReplayPart after = replay.viewOf((2 * 60 + 13) * 1000 + 1);

        end = System.currentTimeMillis();
        System.out.println("Finished splitting after " + (end - start));
        start = end;

        before = studio.squash(before);

        end = System.currentTimeMillis();
        System.out.println("Finished squashing after " + (end - start));
        start = end;

        ReplayPart resultPart = before.append(after);

        end = System.currentTimeMillis();
        System.out.println("Finished connecting parts after " + (end - start));
        start = end;

        Replay result = studio.createReplay(resultPart);

        end = System.currentTimeMillis();
        System.out.println("Finished composing replay after " + (end - start));
        start = end;

        result.save(new File(folder, "result.mcpr"));

        end = System.currentTimeMillis();
        System.out.println("Finished saving after " + (end - start));
    }
}
