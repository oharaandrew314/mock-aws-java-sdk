package io.andrewohara.awsmock.cloudformation

import software.amazon.awssdk.services.cloudformation.CloudFormationClient
import software.amazon.awssdk.services.cloudformation.model.*

class MockCloudformationV2(private val backend: MockCloudformationBackend = MockCloudformationBackend()): CloudFormationClient {

    override fun close() {}
    override fun serviceName() = "cloudformation-mock"

    override fun listExports(listExportsRequest: ListExportsRequest): ListExportsResponse {
        val exports = backend.stacks()
            .flatMap { stack ->
                stack.exports
                    .map { export ->
                        Export.builder()
                            .exportingStackId(stack.id)
                            .name(export.key)
                            .value(export.value)
                            .build()
                    }
            }

        return ListExportsResponse.builder()
            .exports(exports)
            .build()
    }
}