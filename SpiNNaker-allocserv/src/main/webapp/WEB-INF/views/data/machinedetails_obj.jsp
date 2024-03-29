<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="json" uri="http://www.atg.com/taglibs/json" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
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
machine = (
</c:if>
<json:object>
	<json:property name="name" value="${ machine.name }" />
	<json:property name="width" value="${ machine.width }" />
	<json:property name="height" value="${ machine.height }" />
	<json:property name="num_in_use" value="${ machine.numInUse }" />
	<json:array name="tags" items="${ machine.tags }" />
	<json:array name="jobs" items="${ machine.jobs }" var="job">
		<json:object>
			<json:property name="id" value="${ job.id }" />
			<c:if test="${ job.owner.present }">
				<spring:eval var="jobUrl" expression="job.url.get()" />
				<spring:eval var="jobOwner" expression="job.owner.get()" />
				<json:property name="url" value="${ jobUrl }" />
				<json:property name="owner" value="${ jobOwner }" />
			</c:if>
			<json:array name="boards" items="${ job.boards }" var="board">
				<%-- board is a BoardCoords --%>
				<json:object>
					<json:object name="triad">
						<json:property name="x" value="${ board.x }" />
						<json:property name="y" value="${ board.y }" />
						<json:property name="z" value="${ board.z }" />
					</json:object>
					<json:object name="physical">
						<json:property name="cabinet" value="${ board.cabinet }" />
						<json:property name="frame" value="${ board.frame }" />
						<json:property name="board" value="${ board.board }" />
					</json:object>
					<c:if test="${ job.owner.present }">
						<json:object name="network">
							<json:property name="address" value="${ board.address }" />
						</json:object>
					</c:if>
				</json:object>
			</json:array>
		</json:object>
	</json:array>
	<json:array name="live_boards" items="${ machine.live }" var="board">
		<%-- board is a BoardCoords --%>
		<json:object>
			<json:object name="triad">
				<json:property name="x" value="${ board.x }" />
				<json:property name="y" value="${ board.y }" />
				<json:property name="z" value="${ board.z }" />
			</json:object>
			<json:object name="physical">
				<json:property name="cabinet" value="${ board.cabinet }" />
				<json:property name="frame" value="${ board.frame }" />
				<json:property name="board" value="${ board.board }" />
			</json:object>
			<sec:authorize access="hasRole('ADMIN')">
				<json:object name="network">
					<json:property name="address" value="${ board.address }" />
				</json:object>
			</sec:authorize>
		</json:object>
	</json:array>
	<json:array name="dead_boards" items="${ machine.dead }" var="board">
		<%-- board is a BoardCoords --%>
		<json:object>
			<json:object name="triad">
				<json:property name="x" value="${ board.x }" />
				<json:property name="y" value="${ board.y }" />
				<json:property name="z" value="${ board.z }" />
			</json:object>
			<json:object name="physical">
				<json:property name="cabinet" value="${ board.cabinet }" />
				<json:property name="frame" value="${ board.frame }" />
				<json:property name="board" value="${ board.board }" />
			</json:object>
			<sec:authorize access="hasRole('ADMIN')">
				<json:object name="network">
					<json:property name="address" value="${ board.address }" />
				</json:object>
			</sec:authorize>
		</json:object>
	</json:array>
</json:object>
<c:if test="${ empty param.json }">
);
</c:if>
