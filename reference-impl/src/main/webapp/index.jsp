<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">
<%@ page import="org.maera.plugin.Plugin" %>
<%@ page import="org.maera.plugin.refimpl.ContainerManager" %>
<%@ page import="org.osgi.framework.Bundle" %>
<%@ page import="org.osgi.framework.ServiceReference" %>
<html>
<head>
    <meta name="decorator" content="atl.general">
</head>
<h2>Plugins</h2>
<ul>
    <% for (Object plugin : ContainerManager.getInstance().getPluginAccessor().getPlugins()) {
        Plugin p = (Plugin) plugin;
    %>
    <li><%=p.getKey()%>
    </li>
    <% } %>
</ul>

<h2>Bundles</h2>
<table border="1">
    <tr>
        <th>Name</th>
        <th>Version</th>
        <th>State</th>
        <th>Services</th>
    </tr>

    <% for (Bundle bundle : ContainerManager.getInstance().getOsgiContainerManager().getBundles()) {
    %>
    <tr>
        <td>
            <%=bundle.getSymbolicName()%>
        </td>
        <td>
            <%=bundle.getHeaders().get("Bundle-Version")%>
        </td>
        <td>
            <%
                String state = "N/A";
                switch (bundle.getState()) {
                    case Bundle.INSTALLED:
                        state = "Installed";
                        break;
                    case Bundle.ACTIVE:
                        state = "Active";
                        break;
                    case Bundle.RESOLVED:
                        state = "Resolved";
                        break;
                }
            %>
            <%=state%>
        </td>
        <td>
            &nbsp;
            <% if (bundle.getRegisteredServices() != null && bundle.getRegisteredServices().length > 0) { %>
            <table border="1" width="100%">
                <tr>
                    <th>Interfaces</th>
                    <th>Properties</th>
                    <th>Using Bundles</th>
                </tr>

                <% for (ServiceReference service : bundle.getRegisteredServices()) { %>
                <tr>
                    <td>
                        <% String[] infs = (String[]) service.getProperty("objectClass");
                            for (String inf : infs) {%>
                        <li><%=inf%>
                        </li>
                        <% } %>

                    </td>
                    <td>
                        <ul>
                            <% for (String key : service.getPropertyKeys()) {
                                if (!"objectClass".equals(key)) { %>
                            <li><%=key%> - <%=service.getProperty(key)%>
                            </li>
                            <% }
                            } %>
                        </ul>
                    </td>
                    <td> &nbsp;
                        <ul>
                            <% if (service.getUsingBundles() != null) {
                                for (Bundle user : service.getUsingBundles()) {%>

                            <li><%=user.getSymbolicName()%>
                            </li>
                            <% }
                            }%>
                        </ul>
                    </td>
                </tr>

                <% } %>
            </table>
            <% } %>
        </td>
    </tr>

    <% } %>
</table>

</html>