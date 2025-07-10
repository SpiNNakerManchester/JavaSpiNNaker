<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%--
Copyright (c) 2021 The University of Manchester

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

		https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
--%>
<c:if test="${ empty param.json }"><%-- Pass json=true to emit pure JSON --%>
job = (
</c:if>
{
	"id": "${ job.id }",
	<c:if test="${ job.owner.present }">
		<spring:eval var="jobOwner" expression="job.owner.get()" />
		<spring:eval var="jobOwnerHost" expression="job.ownerHost.get()" />
		"owner": "${ jobOwner }",
		"owner_host": "${ jobOwnerHost }",
	</c:if>
	"state": "${ job.state }",
	"start": "${ job.startTime }",
	"keep_alive": "${ job.keepAlive }",
	<c:if test="${ job.width.present }">
		<spring:eval var="jobWidth" expression="job.width.get()" />
		<spring:eval var="jobHeight" expression="job.height.get()" />
		"width": "${ jobWidth }",
		"height": "${ jobHeight }",
	</c:if>
	"powered": "${ job.powered }",
	"machine": "${ job.machine }",
	"machine_url": "${ job.machineUrl }",
	"boards": [
		<c:forEach items="${ job.boards }" var="board" varStatus="loop">
		<%-- board is a BoardCoords --%>
		{
			"triad": {
				"x": "${ board.x }",
				"y": "${ board.y }",
				"z": "${ board.z }"
			},
			"physical": {
				"cabinet": "${ board.cabinet }",
				"frame": "${ board.frame }",
				"board": "${ board.board }"
			},
			<c:if test="${ job.owner.present }">
				"network": {
					"address": "${ board.address }"
				}
			</c:if>
			}<c:if test="${ !loop.last }">,</c:if>
		</c:forEach>
	],
	"triad_width": "${ job.triadWidth }",
	"triad_height": "${ job.triadHeight }"
}
<c:if test="${ empty param.json }">
);
</c:if>
