package net.wti.gradle.schema.spi;

import org.gradle.api.Named;

/**
 * DependencyResolutionPlan:
 * <p><p>
 *     A dependency resolution plan is an object which encapsulates the various stages we need to go through
 *     to get from schema.xapi files on disk to a completed build index and dependency graph ...plan.
 * <p><p>
 *     We do not use this object to download dependencies in gradle, we use it to plan a strict order of parser+callback operations:
 *     <ul>
 *         <li>Parse root schema.xapi file</li>
 *         <li>Parse only schema.xapi files referenced by said root schema</li>
 *         <li>Apply ast-widening (inserting synthetic elements)</li>
 *         <li>Make completed SchemaProject objects fully-initialized</li>
 *         <li>Process user beforeSettings callbacks
 *                <ul>
 *                    <li>Callback should receive a "can provide lazy-initialized IndexViewInitial graph"
 *                          <ul>
 *                              <li>A pre-cursor to SchemaProjects</li>
 *                              <li>only knows about project, platforms and modules (concept of dependencies, just intra-module includes)</li>
 *                              <li>Applies a final ast-widening before freezing the project graph structure</li>
 *                              <li>If user has manually reduces platforms (-DxapiPlatform=jdk11 ought to suffice), we prune them here.</li>
 *                           </ul>
 *                    </li>
 *                    <li>User may see, visit and insert AST of schema.xapi</li>
 *                    <li>Callback should be able to parse new schema.xapi to add to project graph,
 *                          <b>PROVIDED nobody has initialized the project graph yet</b>
 *                    </li>
 *                </ul>
 *         </li>
 *         <li>Start writing "complete index" to disk.
 *             <ul>
 *                 <li>An IndexViewInitial is made available to finalize-on-demand</li>
 *                 <li>The actual processing to spam file-writes is done off-thread.</li>
 *                 <li>Writes files with liveness==1, to form complete, possible dependency graph.</li>
 *                 <li>The vanila index can tell you the following:
 *                      <ul>
 *                          <li>Subscription-based model for all root-schema.xapi-based projects, platforms and modules</li>
 *                          <li>You will be notified once on or before the vanila index is finalized.</li>
 *                          <li>Shows all project+platform+module pairs that must be alive (have source or forced, liveness>=2)</li>
 *                          <li>Shows all project+platform+module pairs that exist ony as interim dependencies (liveness=1)</li>
 *                      </ul>
 *                 </li>
 *                 <li>This vanila index view is made resolvable-on-demand in settings.gradle.</li>
 *             </ul>
 *         </li>
 *         <li>Release control flow to settings.gradle evaluation (xapi-loader plugin finishes)
 *              <ul>
 *                  <li>User may access the IndexViewInitial to query about all known xapi modules and gradle project
 *                      <ul><li>Where number of gradle projects >= number of xapi modules</li></ul>
 *                  </li>
 *                  <li>It is only possible to parse new schema.xapi files until the IndexViewInitial is resolved</li>
 *                  <li>User may add extra flavor to the IndexViewInitial as they please</li>
 *                  <li>User may resolve SchemaProject in a mutable fashion, and insert dependencies
 *                      <ul>
 *                          <li>Resolving complete SchemaProject / SchemaMap freezes the IndexViewInitial</li>
 *                      </ul>
 *                  </li>
 *                  <li>User may opt in to "auto-find schema.xapi files in manually include(':name')'d projects</li>
 *              </ul>
 *         </li>
 *         <li>Process user settings callbacks
 *             <ul>
 *                 <li>User callback receives the complete DependencyResolutionPlan in it's argument (directly, or w/ composition)</li>
 *                 <li>User may view the mutable SchemaProject structure to do ...whatever they want</li>
 *                 <li>A finalizable "fully pruned" index is made available-to-resolve
 *                      <ul>
 *                          <li>Resolving the FinalIndex causes all SchemaMap/SchemaProject objects to become immutable</li>
 *                          <li>Contains the complete "gospel truth" of the full (optionally pruned) dependency graph</li>
 *                          <li>All possible gradle projects that we know about will need to be created, but not yet finalized</li>
 *                      </ul>
 *                 </li>
 *             </ul>
 *         </li>
 *         <li>Prune / update index for anything user has configured
 *              <ul>
 *                      <li>If user has not realized the FinalIndex, it will be running in the background</li>
 *                      <li>Cleans up all old files, optionally erases "dependencies-only" nodes (liveness=1)</li>
 *              </ul>
 *         </li>
 *         <li>Process after settings callbacks.
 *              <ul>
 *                      <li>Callback receives an immutable, read-only, complete, synchronous-view of schema graph is available</li>
 *                      <li>No new dependencies may be added, but you may directly read all valid metadata</li>
 *              </ul>
 *
 *         </li>
 *     </ul>
 * Created by James X. Nelson (James@WeTheInter.net) on 08/05/2021 @ 2:43 a.m..
 */
public interface DependencyResolutionPlan {

    interface SchemaStage extends Named {}
    enum DefaultSchemaStage implements SchemaStage {
        ParseSchemaPlan,
        ParseRootSchemaPlan,
        ApplyAstWidening,
        SchemaProjectReady,
        BeforeSettingsCallback,
        BeginIndex,
        FinishSettings_Gradle,
        SettingsCallback,
        PruneIndex,
        FinalizeIndex,
        AfterSettingsCallback
        ;

        @Override
        public String getName() {
            return name();
        }
    }

    default SchemaStage[] getStages() {
        return DefaultSchemaStage.values();
    }

}
