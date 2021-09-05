import software.amazon.awssdk.services.s3.S3Client

fun main() {
    val s3 = S3Client.create()

    val res = s3.getObject {
        it.bucket("121o23904583454")
        it.key("sdofjds")
    }

    println(res)
}