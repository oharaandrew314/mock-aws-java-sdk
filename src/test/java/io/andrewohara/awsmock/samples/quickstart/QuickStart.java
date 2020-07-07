package io.andrewohara.awsmock.samples.quickstart;

import com.amazonaws.services.s3.AmazonS3;

import java.util.List;
import java.util.stream.Collectors;

public class QuickStart {

    private final AmazonS3 s3Client;
    private final String bucket;

    public QuickStart(String bucket, AmazonS3 s3Client) {
        this.bucket = bucket;
        this.s3Client = s3Client;
    }

    public List<String> process() {
        return s3Client.listObjectsV2(bucket)
                .getObjectSummaries()
                .stream()
                .map(summary -> s3Client.getObjectAsString(bucket, summary.getKey()))
                .collect(Collectors.toList());
    }
}
