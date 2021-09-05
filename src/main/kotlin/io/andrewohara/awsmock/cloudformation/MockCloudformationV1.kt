package io.andrewohara.awsmock.cloudformation

import com.amazonaws.services.cloudformation.AbstractAmazonCloudFormation
import com.amazonaws.services.cloudformation.model.Export
import com.amazonaws.services.cloudformation.model.ListExportsRequest
import com.amazonaws.services.cloudformation.model.ListExportsResult

class MockCloudformationV1(private val backend: MockCloudformationBackend): AbstractAmazonCloudFormation() {

    override fun listExports(listExportsRequest: ListExportsRequest): ListExportsResult {
        val exports = backend.stacks().flatMap { stack ->
            stack.exports.map { (key, value) ->
                Export().withExportingStackId(stack.id).withName(key).withValue(value)
            }
        }

        return ListExportsResult().withExports(exports)
    }
}