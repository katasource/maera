/**
 *
 * This package contains events that will be fired by the framework.
 * <p/>
 * The most useful events are those concerning plugin and modules.  This table gives an overview on the key events
 * and when they will and will not fire:
 *
 * <table class="confluenceTable"><tbody>
 * <tr>
 * <th class="confluenceTh"> Event </th>
 * <th class="confluenceTh"> When fired </th>
 * <th class="confluenceTh"> When not </th>
 * </tr>
 * <tr>
 * <td class="confluenceTd"> PluginEnabledEvent </td>
 * <td class="confluenceTd"><ul>
 * 	<li>Plugin explicitly enabled via PluginController</li>
 * 	<li>Plugin refreshed as a result of the upgrade of a plugin that provides a package to it</li>
 * 	<li>Plugin enabled after being installed via startup and dynamic installation</li>
 * </ul>
 * </td>
 * <td class="confluenceTd"><ul>
 * 	<li>Plugin enabled by starting OSGi bundle directly</li>
 * 	<li>OSGi bundle installed via OSGi apis</li>
 * </ul>
 * </td>
 * </tr>
 * <tr>
 * <td class="confluenceTd"> PluginDisabledEvent <br class="atl-forced-newline"> </td>
 * <td class="confluenceTd"><ul>
 * 	<li>Explicitly disabled via PluginController</li>
 * 	<li>A required package dependency is uninstalled</li>
 * 	<li>Stopping an OSGi bundle directly</li>
 * 	<li>A required package dependency is uninstalled</li>
 * 	<li>A required OSGi service dependency is gone.&nbsp; Includes imported  components</li>
 * </ul>
 * </td>
 * <td class="confluenceTd"><ul>
 * 	<li>Plugin framework shutdown</li>
 * </ul>
 * </td>
 * </tr>
 * <tr>
 * <td class="confluenceTd"> PluginModuleEnabledEvent <br class="atl-forced-newline"> </td>
 * <td class="confluenceTd"><ul>
 * 	<li>Explicitly enabled via PluginController</li>
 * 	<li>Plugin enablement during startup and dynamic installation</li>
 * 	<li>Plugin refreshed as a result of the upgrade of a plugin that provides a  package to it</li>
 * 	<li>Explicit plugin enablement via PluginController</li>
 * </ul>
 * </td>
 * <td class="confluenceTd"><ul>
 * 	<li>Plugin enabled via starting OSGi bundle directly</li>
 * 	<li>﻿OSGi bundle installed via OSGi apis</li>
 * </ul>
 * </td>
 * </tr>
 * <tr>
 * <td class="confluenceTd"> PluginModuleDisabledEvent <br class="atl-forced-newline"> </td>
 * <td class="confluenceTd"><ul>
 * 	<li>Explicitly disabled via PluginController</li>
 * 	<li>Explicit  plugin disablement via PluginController</li>
 * 	<li>Plugin refreshed as a result of the upgrade of a plugin that provides a  package to it</li>
 * </ul>
 * </td>
 * <td class="confluenceTd"><ul>
 * 	<li>A required OSGi service dependency is gone.&nbsp; Includes imported  components</li>
 * 	<li>A required package dependency is uninstalled</li>
 * 	<li>Stopping  OSGi bundle directly</li>
 * 	<li>Removal of ModuleDescriptor exposed as an OSGi service</li>
 * 	<li>Plugin framework shutdown</li>
 * 	<li>Failure loading subsequent plugin module exposed as osgi service</li>
 * </ul>
 * </td>
 * </tr>
 * <tr>
 * <td class="confluenceTd"> ﻿PluginUninstalledEvent <br class="atl-forced-newline"> </td>
 * <td class="confluenceTd"><ul>
 * 	<li>Explicitly uninstalled via PluginController</li>
 * </ul>
 * </td>
 * <td class="confluenceTd"><ul>
 * 	<li>Plugin upgrade (process uninstalls old before installing new)</li>
 * </ul>
 * </td>
 * </tr>
 * </tbody></table>

 */
package org.maera.plugin.event.events;