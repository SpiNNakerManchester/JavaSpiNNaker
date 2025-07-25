<%@ page contentType="application/json; encoding=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%--
Copyright (c) 2023 The University of Manchester

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

		http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
--%>
{
	"board_temperature": "${ tempdata.tempTop }",
	<c:if test="${ not empty tempdata.tempExt1 }">
				"backplane_temperature": "${ tempdata.tempExt1 }"
	</c:if>
}
