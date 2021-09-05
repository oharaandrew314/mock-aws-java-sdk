package io.andrewohara.awsmock.samples.cloudformation

import software.amazon.awssdk.services.cloudformation.CloudFormationClient

class ConfigLoader(private val cfn: CloudFormationClient) {

    operator fun invoke(): Config {
        val exports = cfn.listExports().exports().associate {
            it.name() to it.value()
        }

        return Config(
            eventsTopicArn = exports.getValue("users-events-topic-arn"),
            usersTableName = exports.getValue("users-table-name")
        )
    }
}