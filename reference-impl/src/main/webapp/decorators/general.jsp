<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">

<%@ taglib uri="http://www.opensymphony.com/sitemesh/decorator" prefix="decorator" %>

<html>
<head>
    <title><decorator:title default="Welcome!"/></title>
    <decorator:head/>
    <%
        org.maera.plugin.refimpl.ContainerManager.getInstance().getWebResourceManager().includeResources(out);
    %>
</head>

<body>
<h2>
    Atlassian Plugins Reference Implementation - General Decorator
</h2>
<decorator:body/>
<hr>
<div style="text-align:center">
    Atlassian Plugins -
    <a href="http://jira.atlassian.com/browse/PLUG">Issues</a> |
    <a href="http://bamboo.developer.atlassian.com/browse/ATLASSIANPLUGINS">Builds</a> |
    <a href="http://confluence.atlassian.com/display/PLUGINFRAMEWORK">Documentation</a>
</div>
</body>
</html>