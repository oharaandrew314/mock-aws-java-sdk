package io.andrewohara.awsmock.samples.quickstart;

import com.amazonaws.services.s3.AmazonS3;
import io.andrewohara.awsmock.s3.MockS3V1;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class QuickStartUnitTest {

    private final AmazonS3 s3Client = new MockS3V1();
    private final QuickStart testObj = new QuickStart("bucket", s3Client);

    @Test
    public void processTwoFiles() {
        // initialize state
        s3Client.createBucket("bucket");
        s3Client.putObject("bucket", "file1.txt", "special content");
        s3Client.putObject("bucket", "file2.txt", "secret content");

        // perform test
        final List<String> result = testObj.process();

        // verify result
        Assertions.assertThat(result).containsExactlyInAnyOrder("special content", "secret content");
    }
}