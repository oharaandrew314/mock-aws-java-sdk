import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import com.amazonaws.services.sqs.model.CreateQueueRequest
import com.amazonaws.services.sqs.model.ReceiveMessageRequest

fun main() {
    val sqs = AmazonSQSClientBuilder.defaultClient()

    val request = ReceiveMessageRequest()
        .withQueueUrl("https://sqs.us-east-1.amazonaws.com/583125843759/foo")
        .withMaxNumberOfMessages(100)
    val result = sqs.receiveMessage(request)

    println(result)
}