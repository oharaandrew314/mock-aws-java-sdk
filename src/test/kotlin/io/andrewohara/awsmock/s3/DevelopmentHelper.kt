package io.andrewohara.awsmock.s3

import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.GetObjectRequest
import org.junit.Ignore
import org.junit.Test
import java.nio.file.Files
import java.time.Instant
import java.util.*

@Ignore
class DevelopmentHelper {

    private val client = AmazonS3ClientBuilder.defaultClient()

    private val bucketName = "io.andrewohara.foo"

    @Test
    fun createBucket() {
        val resp = client.createBucket(bucketName)
        println(resp)
        println(resp == null)
    }

    @Test
    fun putObject() {
        client.putObject(bucketName, "foo", "bar")
    }

    @Test
    fun getObject() {
        val resp = client.getObject(bucketName, "foo")
        println(resp)
    }

    @Test
    fun deleteObject() {
        val resp = client.deleteObject(bucketName, "foo")
        println(resp)
    }

    @Test
    fun getMetadata() {
        val resp = client.getObjectMetadata(bucketName, "foo")
        println(resp)
    }

    @Test
    fun deleteBucket() {
        val resp = client.deleteBucket(bucketName)
        println(resp)

//        Assertions.assertThatThrownBy {
//            client.deleteBucket(bucketName)
//        }.isInstanceOf(AmazonS3Exception::class.java)
//                .hasMessageContaining("The specified bucket does not exist (Service: Amazon S3; Status Code: 404; Error Code: NoSuchBucket;")
    }

    @Test
    fun copyObject() {
        client.copyObject(bucketName, "foo", bucketName, "foo")
    }

    @Test
    fun saveToFile() {
        val dest = Files.createTempFile("foo", ".txt")
        dest.toFile().deleteOnExit()
        val request = GetObjectRequest(bucketName, "foo")

        client.getObject(request, dest.toFile())
    }

    @Test
    fun listObjects() {
        val resp = client.listObjects("io.andrewohara.bar")
        println(resp)
    }

    @Test
    fun presignedUrl() {
        val resp = client.generatePresignedUrl("io.andrewohara.bar", "foo", Date.from(Instant.now().plusSeconds(120)))
        println(resp)
    }

    @Test
    fun doesObjectExist() {
        val resp = client.doesObjectExist("io.andrewohara.bar", "foo")
        println(resp)
    }

    @Test
    fun getObjectAsString() {
        client.getObjectAsString(bucketName, "lol")
    }
}