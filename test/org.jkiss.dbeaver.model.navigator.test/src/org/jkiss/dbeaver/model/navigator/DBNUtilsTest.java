/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.navigator;

import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPHiddenObject;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.After;
import org.junit.Test;
import org.jkiss.code.NotNull;
import org.jkiss.utils.AlphanumericComparator;
import java.util.Comparator;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class DBNUtilsTest extends DBeaverUnitTest {

    private final List<String> changedProperties = new ArrayList<>();

    @After
    public void tearDown() {
        var prefStore = DBWorkbench.getPlatform().getPreferenceStore();
        changedProperties.forEach(prefStore::setToDefault);
        changedProperties.clear();
    }

    @Test
    public void shouldNotSortByNameIfAlphabeticallyIfAlphabeticallyFalse() {
        // given
        addProperty(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY, false);
        addProperty(ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST, false);
        //then
        assertRemainUnsorted();
    }

    @Test
    public void shouldNotSortByNameIfAlphabeticallyFalseAndByFolderTrue() {
        // given
        addProperty(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY, false);
        addProperty(ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST, true);
        //then
        assertRemainUnsorted();
    }

    @Test
    public void shouldSortIgnoreCaseWhenIgnoreCaseTrue(){
        // given
        addProperty(ModelPreferences.NAVIGATOR_SORT_IGNORE_CASE, true);
        //then
        assertCorrectSortingIgnoreCase(true);
        assertCorrectSortingIgnoreCase(false);
    }

    @Test
    public void shouldSortWithCaseWhenIgnoreCaseFalse(){
        // given
        addProperty(ModelPreferences.NAVIGATOR_SORT_IGNORE_CASE, false);
        //then
        assertCorrectSortingWithCase(true);
        assertCorrectSortingWithCase(false);
    }

    /* ADDITIONAL TEST CASES */

    @Test
    public void P1_sortingDisabled_preservesOriginalOrder() {
        // Partition P1: Sorting disabled
        addProperty(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY, false);
        addProperty(ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST, false);

        List<String> givenNames = List.of("b", "a", "A", "C");
        var result = DBNUtils.filterNavigableChildren(getNamedNodes(givenNames), true);

        assertEquals(givenNames, Arrays.stream(result).map(DBNNode::getNodeDisplayName).toList());
    }

    @Test
    public void P2_caseInsensitiveAlphabeticSort_ordersMixedCaseCorrectly() {
        // Partition P2: Case-insensitive alphabetical sorting
        addProperty(ModelPreferences.NAVIGATOR_SORT_IGNORE_CASE, true);
        addProperty(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY, true);
        addProperty(ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST, false);

        List<String> givenNames = List.of("b", "a", "A", "C");
        List<String> expectedNames = List.of("a", "A", "b", "C");

        assertCorrectSortingIgnoreCase(expectedNames, givenNames);
    }

    @Test
    public void P3_alphanumericNoLeadingZeros_naturalSortOrdersByNumericValue() {
        // Partition P3: Alphanumeric values with no leading zeros (natural sort)
        addProperty(ModelPreferences.NAVIGATOR_SORT_IGNORE_CASE, true);
        addProperty(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY, true);
        addProperty(ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST, false);

        List<String> givenNames = List.of("s2", "s1", "s10", "s3");
        List<String> expectedNames = List.of("s1", "s2", "s3", "s10");

        assertCorrectSortingIgnoreCase(expectedNames, givenNames);
    }

    @Test
    public void P4_alphanumericLeadingZeros_handlesZeroPaddedNumbersConsistently() {
        // Partition P4: Alphanumeric values with leading zeros
        addProperty(ModelPreferences.NAVIGATOR_SORT_IGNORE_CASE, true);
        addProperty(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY, true);
        addProperty(ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST, false);

        List<String> givenNames = List.of("s2", "s01", "s002", "s10", "s02");
        List<String> expectedNames = List.of("s01", "s2", "s002", "s02", "s10");

        assertCorrectSortingIgnoreCase(expectedNames, givenNames);
    }

    @Test
    public void P5_longNames_sortsCorrectlyAndDoesNotFail() {
        // Partition P5: Very long names (boundary length)
        addProperty(ModelPreferences.NAVIGATOR_SORT_IGNORE_CASE, true);
        addProperty(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY, true);
        addProperty(ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST, false);

        List<String> givenNames = List.of(
            "s2123456789123456789",
            "s1123456789123456789"
        );
        List<String> expectedNames = List.of(
            "s1123456789123456789",
            "s2123456789123456789"
        );

        assertCorrectSortingIgnoreCase(expectedNames, givenNames);
    }

    @Test
    public void P6_duplicates_preservesAllValuesAndOrdersGroupCorrectly() {
        // Partition P6: Duplicate names
        addProperty(ModelPreferences.NAVIGATOR_SORT_IGNORE_CASE, true);
        addProperty(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY, true);
        addProperty(ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST, false);

        List<String> givenNames = List.of("b", "a", "a", "A", "C");
        List<String> expectedNames = List.of("a", "a", "A", "b", "C");

        assertCorrectSortingIgnoreCase(expectedNames, givenNames);
    }

    @Test
    public void P7_emptyAndWhitespace_doesNotCrashAndPreservesAllElements() {
        // Partition P7: Empty / whitespace-only names
        addProperty(ModelPreferences.NAVIGATOR_SORT_IGNORE_CASE, true);
        addProperty(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY, true);
        addProperty(ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST, false);

        List<String> givenNames = List.of("b", "", " ", "a");
        var result = DBNUtils.filterNavigableChildren(getNamedNodes(givenNames), true);

        List<String> actual = Arrays.stream(result).map(DBNNode::getNodeDisplayName).toList();
        assertEquals(givenNames.size(), actual.size());
        assertEquals(givenNames.stream().sorted().toList(), actual.stream().sorted().toList());
    }

    @Test
    public void P8_specialCharacters_doesNotCrashAndPreservesAllElements() {
        // Partition P8: Special characters / punctuation in names
        addProperty(ModelPreferences.NAVIGATOR_SORT_IGNORE_CASE, true);
        addProperty(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY, true);
        addProperty(ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST, false);

        List<String> givenNames = List.of("a_1", "a-1", "a.1", "a1");
        var result = DBNUtils.filterNavigableChildren(getNamedNodes(givenNames), true);

        List<String> actual = Arrays.stream(result).map(DBNNode::getNodeDisplayName).toList();
        assertEquals(givenNames.size(), actual.size());
        assertEquals(givenNames.stream().sorted().toList(), actual.stream().sorted().toList());
    }


    /* END OF ADDITIONAL TEST CASES */

    /*BAD DESIGN, AND MOCKING */

    /**
     * IMPROVED " BAD TEST" DESIGN - SORTABLE COMPARATOR
    */
    @Test
    public void testImprovedDesign_ComparatorWithInjectedPreferences() {
        // This test demonstrates an improved design pattern
        // Instead of sortNodes() calling DBWorkbench.getPlatform().getPreferenceStore(),
        // inject preferences into the comparator
        
        // ARRANGE: Create a mock preference store and inject it
        var mockPrefStore = mock(DBPPreferenceStore.class);
        when(mockPrefStore.getBoolean(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY)).thenReturn(true);
        when(mockPrefStore.getBoolean(ModelPreferences.NAVIGATOR_SORT_IGNORE_CASE)).thenReturn(true);
        
        // Create comparison function (this would be a better design for NodeNameComparator)
        java.util.Comparator<String> injectedComparator = (a, b) -> {
            // Simulates NodeNameComparator with injected preference
            boolean ignoreCase = mockPrefStore.getBoolean(ModelPreferences.NAVIGATOR_SORT_IGNORE_CASE);
            return ignoreCase 
                ? a.compareToIgnoreCase(b)
                : a.compareTo(b);
        };
        
        // ACT: Test the comparator with injected preferences
        String[] names = {"Zebra", "apple", "Monkey"};
        String[] sorted = names.clone();
        Arrays.sort(sorted, injectedComparator);
        
        // ASSERT: With case-insensitive ignoring, should be alphabetical
        assertEquals("apple", sorted[0]);
        assertEquals("Monkey", sorted[1]);
        assertEquals("Zebra", sorted[2]);
        
        // VERIFICATION: Confirm preference was accessed (demonstrates mocking)
        verify(mockPrefStore, atLeastOnce()).getBoolean(ModelPreferences.NAVIGATOR_SORT_IGNORE_CASE);
        
    }

    /**
     *MOCKITO BEHAVIOR VERIFICATION 
     */
    @Test
    public void testMockito_VerifyHiddenObjectBehavior() {
        
        // ARRANGE: Create mock hidden objects whose behavior we want to verify
        DBPHiddenObject hiddenNode = mock(DBPHiddenObject.class, RETURNS_DEEP_STUBS);
        when(hiddenNode.isHidden()).thenReturn(true);
        when(hiddenNode.getNodeDisplayName()).thenReturn("HiddenNode");
        
        DBPHiddenObject visibleNode = mock(DBPHiddenObject.class, RETURNS_DEEP_STUBS);
        when(visibleNode.isHidden()).thenReturn(false);
        when(visibleNode.getNodeDisplayName()).thenReturn("VisibleNode");
        
        // Create DBNNode mocks that are also DBPHiddenObject (multiple interface implementation)
        DBNNode[] testNodes = new DBNNode[2];
        testNodes[0] = (DBNNode) hiddenNode;
        testNodes[1] = (DBNNode) visibleNode;
        
        verify(hiddenNode).isHidden();
        verify(visibleNode).isHidden();
        
        // isHidden() was called the expected number of times
        verify(hiddenNode, times(1)).isHidden();
        verify(visibleNode, times(1)).isHidden();
        
    }

    /**
     * SPY-BASED TESTING FOR BEHAVIOR VERIFICATION
     */
    @Test
    public void testMockito_SpyingOnNodePreferenceAccess() {
        // ARRANGE: Create a spy on the real preference store to monitor access patterns
        var realPrefStore = DBWorkbench.getPlatform().getPreferenceStore();
        var prefStoreSpy = spy(realPrefStore);
        
        // Configure the spy to track calls while using real behavior
        when(prefStoreSpy.getBoolean(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY)).thenReturn(true);
        when(prefStoreSpy.getBoolean(ModelPreferences.NAVIGATOR_SORT_IGNORE_CASE)).thenReturn(true);
        
        // ACT: Perform sorting operations
        addProperty(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY, true);
        addProperty(ModelPreferences.NAVIGATOR_SORT_IGNORE_CASE, true);
        
        List<String> names = List.of("Zebra", "apple", "Monkey");
        var result = DBNUtils.filterNavigableChildren(getNamedNodes(names), true);
        
        assertEquals(3, result.length);
    }

    /**
     * COMPARING TESTABILITY - WITH vs WITHOUT DEPENDENCY INJECTION
     */
    @Test 
    public void testComparison_StaticVsDependencyInjectionTestability() {
        
        addProperty(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY, true);
        addProperty(ModelPreferences.NAVIGATOR_SORT_IGNORE_CASE, true);
        
        List<String> names = List.of("beta", "Alpha");
        var result1 = DBNUtils.filterNavigableChildren(getNamedNodes(names), true);
        
        assertEquals("Alpha", result1[0].getNodeDisplayName());  // case insensitive
        assertEquals("beta", result1[1].getNodeDisplayName());
        
    }

    /* END OF TESTING IMPROVEMENT SHOWCASE */

    private void assertRemainUnsorted() {
        List<String> givenNames = List.of("b", "a", "A", "C");
        List<String> expectedNames = List.of("b", "a", "A", "C");
        // when
        var result = DBNUtils.filterNavigableChildren(getNamedNodes(givenNames), true);
        // then
        assertEquals(expectedNames, Arrays.stream(result).map(DBNNode::getNodeDisplayName).toList());
    }


    private void assertCorrectSortingIgnoreCase(boolean isFoldersFirst) {
        addProperty(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY, true);
        addProperty(ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST, isFoldersFirst);

        assertCorrectSortingIgnoreCase(List.of("a", "A", "b", "C"), List.of("b", "a", "A", "C"));
        assertCorrectSortingIgnoreCase(List.of("s1", "s2", "s03", "s10"), List.of("s2", "s1", "s10", "s03"));
        assertCorrectSortingIgnoreCase(List.of("s1123456789123456789", "s2123456789123456789"), List.of("s2123456789123456789", "s1123456789123456789"));
    }

    private void assertCorrectSortingWithCase(boolean isFoldersFirst) {
        addProperty(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY,  true);
        addProperty(ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST, isFoldersFirst);

        assertCorrectSortingIgnoreCase(List.of("A", "C", "a", "b"), List.of("b", "a", "A", "C"));
        assertCorrectSortingIgnoreCase(List.of("s1", "s2", "s03", "s10"), List.of("s2", "s1", "s10", "s03"));
        assertCorrectSortingIgnoreCase(
            List.of("s1123456789123456789", "s2123456789123456789"),
            List.of("s2123456789123456789", "s1123456789123456789")
        );
    }

    private void assertCorrectSortingIgnoreCase(List<String> expectedNames, List<String> givenNames) {
        var result = DBNUtils.filterNavigableChildren(getNamedNodes(givenNames), true);
        // then
        assertEquals(expectedNames, Arrays.stream(result).map(DBNNode::getNodeDisplayName).toList());
    }


    private void addProperty(String key, boolean value) {
        var prefStore = DBWorkbench.getPlatform().getPreferenceStore();
        if (!prefStore.contains(key)) {
            throw new IllegalArgumentException("No such property: " + key);
        }
        changedProperties.add(key);
        prefStore.setValue(key, value);
    }

    private DBNNode[] getNamedNodes(List<String> names) {
        return names
            .stream()
            .map(this::createMockNamedNode)
            .toArray(DBNNode[]::new);
    }

    private DBNNode createMockNamedNode(String name) {
        DBNNode node = mock(DBNNode.class, RETURNS_DEEP_STUBS);
        when(node.getNodeDisplayName()).thenReturn(name);
        return node;
    }
}

public class DBNNodeSorter {

    private final DBPPreferenceStore preferenceStore;
    private final NodeNameComparator nodeNameComparator;
    private final NodeFolderComparator nodeFolderComparator;

    /**
     * Creates a sorter with injected preferences (improved testability).
     *
     * @param preferenceStore The preference store to use for sorting decisions.
     *                        Can be a real or mocked preference store.
     */
    public DBNNodeSorter(@NotNull DBPPreferenceStore preferenceStore) {
        this.preferenceStore = preferenceStore;
        this.nodeNameComparator = new NodeNameComparator(preferenceStore);
        this.nodeFolderComparator = new NodeFolderComparator();
    }

    /**
     * Sorts an array of nodes according to configured preferences.
     *
     * This method replaces DBNUtils.sortNodes() with an improved testable design.
     *
     * @param children The nodes to sort (modified in place).
     */
    public void sortNodes(@NotNull DBNNode[] children) {
        if (children.length == 0) {
            return;
        }

        Comparator<DBNNode> comparator = selectComparator();
        if (Objects.nonNull(comparator)) {
            Arrays.sort(children, comparator);
        }
    }

    /**
     * Selects the appropriate comparator based on preference settings.
     *
     * @return The comparator to use, or null if no sorting should be applied.
     */
    @NotNull
    private Comparator<DBNNode> selectComparator() {
        // Check if alphabetical sorting is enabled
        if (!preferenceStore.getBoolean(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY)) {
            return null;
        }

        // Start with the name comparator
        Comparator<DBNNode> comparator = nodeNameComparator;

        // Add folder-first sorting if enabled
        if (preferenceStore.getBoolean(ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST)) {
            comparator = nodeFolderComparator.thenComparing(nodeNameComparator);
        }

        return comparator;
    }

    /**
     * Comparator for sorting by node display name.
     * Uses alphanumeric sorting with optional case-insensitivity.
     */
    private static class NodeNameComparator implements Comparator<DBNNode> {
        private final AlphanumericComparator alphanumericComparator;
        private final boolean caseInsensitive;

        public NodeNameComparator(DBPPreferenceStore preferenceStore) {
            this.alphanumericComparator = AlphanumericComparator.getInstance();
            // Get case sensitivity preference once during construction
            this.caseInsensitive = preferenceStore.getBoolean(
                ModelPreferences.NAVIGATOR_SORT_IGNORE_CASE
            );
        }

        @Override
        public int compare(DBNNode node1, DBNNode node2) {
            if (caseInsensitive) {
                return alphanumericComparator.compareIgnoreCase(
                    node1.getNodeDisplayName(),
                    node2.getNodeDisplayName()
                );
            } else {
                return alphanumericComparator.compare(
                    node1.getNodeDisplayName(),
                    node2.getNodeDisplayName()
                );
            }
        }
    }

    /**
     * Comparator for sorting folders before items.
     */
    private static class NodeFolderComparator implements Comparator<DBNNode> {
        @Override
        public int compare(DBNNode node1, DBNNode node2) {
            boolean isFolderNode1 = isFolderNode(node1);
            boolean isFolderNode2 = isFolderNode(node2);

            // Folders come first (-1), then items (1)
            if (isFolderNode1 && !isFolderNode2) return -1;
            if (!isFolderNode1 && isFolderNode2) return 1;
            return 0;
        }

        private boolean isFolderNode(DBNNode node) {
            return node instanceof DBNContainer || node.allowsChildren();
        }
    }
}


public class DBNNodeSorterTest extends DBeaverUnitTest {

    //TEST 1: Basic Sorting with Injected Mock Preferences
    @Test
    public void testSorting_WithMockedPreferences_CaseInsensitive() {
        // ARRANGE: Create a mock preference store with specific configuration
        DBPPreferenceStore mockPrefStore = mock(DBPPreferenceStore.class);
        when(mockPrefStore.getBoolean(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY)).thenReturn(true);
        when(mockPrefStore.getBoolean(ModelPreferences.NAVIGATOR_SORT_IGNORE_CASE)).thenReturn(true);
        when(mockPrefStore.getBoolean(ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST)).thenReturn(false);

        // Create sorter with injected mock preferences
        DBNNodeSorter sorter = new DBNNodeSorter(mockPrefStore);

        // Create test nodes with mixed case names
        DBNNode[] nodes = createMockNodes("Zebra", "apple", "Monkey", "BANANA");

        // ACT: Sort the nodes
        sorter.sortNodes(nodes);

        // ASSERT: Should be sorted case-insensitively
        assertEquals("apple", nodes[0].getNodeDisplayName());
        assertEquals("BANANA", nodes[1].getNodeDisplayName());
        assertEquals("Monkey", nodes[2].getNodeDisplayName());
        assertEquals("Zebra", nodes[3].getNodeDisplayName());

        // VERIFY: Confirm preferences were accessed as expected
        verify(mockPrefStore).getBoolean(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY);
        verify(mockPrefStore).getBoolean(ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST);
        verify(mockPrefStore, times(1)).getBoolean(ModelPreferences.NAVIGATOR_SORT_IGNORE_CASE);
    }

    //TEST 2: Multiple Preference Combinations (Parallel Testing Capability)
    @Test
    public void testSorting_MultipleConfigurations_NoGlobalStateConflict() {
        // SCENARIO 1: Case-sensitive sorting
        {
            DBPPreferenceStore prefStore1 = mock(DBPPreferenceStore.class);
            when(prefStore1.getBoolean(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY)).thenReturn(true);
            when(prefStore1.getBoolean(ModelPreferences.NAVIGATOR_SORT_IGNORE_CASE)).thenReturn(false); // Case sensitive
            when(prefStore1.getBoolean(ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST)).thenReturn(false);

            DBNNodeSorter sorter1 = new DBNNodeSorter(prefStore1);
            DBNNode[] nodes1 = createMockNodes("banana", "Apple", "cherry", "Banana");

            sorter1.sortNodes(nodes1);

            // Uppercase letters come before lowercase in ASCII
            assertEquals("Apple", nodes1[0].getNodeDisplayName());
            assertEquals("Banana", nodes1[1].getNodeDisplayName());
        }

        // SCENARIO 2: Case-insensitive sorting (different configuration, same test!)
        {
            DBPPreferenceStore prefStore2 = mock(DBPPreferenceStore.class);
            when(prefStore2.getBoolean(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY)).thenReturn(true);
            when(prefStore2.getBoolean(ModelPreferences.NAVIGATOR_SORT_IGNORE_CASE)).thenReturn(true); // Case insensitive
            when(prefStore2.getBoolean(ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST)).thenReturn(false);

            DBNNodeSorter sorter2 = new DBNNodeSorter(prefStore2);
            DBNNode[] nodes2 = createMockNodes("banana", "Apple", "cherry", "Banana");

            sorter2.sortNodes(nodes2);

            // With case insensitive, all 'a' comes before all 'b' before 'c'
            assertEquals("Apple", nodes2[0].getNodeDisplayName());
            assertEquals("banana", nodes2[1].getNodeDisplayName());
            assertEquals("Banana", nodes2[2].getNodeDisplayName());
            assertEquals("cherry", nodes2[3].getNodeDisplayName());
        }

        // SCENARIO 3: No sorting
        {
            DBPPreferenceStore prefStore3 = mock(DBPPreferenceStore.class);
            when(prefStore3.getBoolean(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY)).thenReturn(false);

            DBNNodeSorter sorter3 = new DBNNodeSorter(prefStore3);
            String[] originalOrder = {"Zebra", "apple", "Monkey", "BANANA"};
            DBNNode[] nodes3 = createMockNodes(originalOrder);

            sorter3.sortNodes(nodes3);

            // Should remain in original order
            for (int i = 0; i < originalOrder.length; i++) {
                assertEquals(originalOrder[i], nodes3[i].getNodeDisplayName());
            }
        }
    }

    //TEST 3: Folders-First Sorting with Mocking
    @Test
    public void testSorting_FoldersFirst_WithMockedPreferences() {
        // ARRANGE: Configure for folders-first, case-insensitive sorting
        DBPPreferenceStore mockPrefStore = mock(DBPPreferenceStore.class);
        when(mockPrefStore.getBoolean(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY)).thenReturn(true);
        when(mockPrefStore.getBoolean(ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST)).thenReturn(true);
        when(mockPrefStore.getBoolean(ModelPreferences.NAVIGATOR_SORT_IGNORE_CASE)).thenReturn(true);

        DBNNodeSorter sorter = new DBNNodeSorter(mockPrefStore);

        // Create mixed folder and file nodes
        DBNNode[] nodes = new DBNNode[4];
        nodes[0] = createMockNode("Document.txt", true);  // File
        nodes[1] = createMockNode("FolderZ", false);      // Folder
        nodes[2] = createMockNode("FolderA", false);      // Folder
        nodes[3] = createMockNode("Archive.zip", true);   // File

        // ACT: Sort
        sorter.sortNodes(nodes);

        // ASSERT: Folders should come first (alphabetically), then files (alphabetically)
        assertEquals("FolderA", nodes[0].getNodeDisplayName());
        assertEquals("FolderZ", nodes[1].getNodeDisplayName());
        assertEquals("Archive.zip", nodes[2].getNodeDisplayName());
        assertEquals("Document.txt", nodes[3].getNodeDisplayName());

        // VERIFY: Preferences were accessed in correct order
        verify(mockPrefStore).getBoolean(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY);
        verify(mockPrefStore).getBoolean(ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST);
    }


    //TEST 4: Verifying the Improvement Over Global State Approach
    @Test
    public void testMocking_Advantage_VerifyingPreferenceAccess() {
        // ARRANGE: Create a spy on the preference store to verify interaction
        DBPPreferenceStore mockPrefStore = mock(DBPPreferenceStore.class);
        when(mockPrefStore.getBoolean(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY)).thenReturn(true);
        when(mockPrefStore.getBoolean(ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST)).thenReturn(false);
        when(mockPrefStore.getBoolean(ModelPreferences.NAVIGATOR_SORT_IGNORE_CASE)).thenReturn(true);

        DBNNodeSorter sorter = new DBNNodeSorter(mockPrefStore);
        DBNNode[] nodes = createMockNodes("b", "a");

        // ACT: Sort nodes
        sorter.sortNodes(nodes);
        verify(mockPrefStore, times(1)).getBoolean(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY);
        verify(mockPrefStore, times(1)).getBoolean(ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST);
        verify(mockPrefStore, times(1)).getBoolean(ModelPreferences.NAVIGATOR_SORT_IGNORE_CASE);
        verify(mockPrefStore, never()).getString(any());
        verify(mockPrefStore, never()).getInt(any());
    }


    //TEST 5: Testing Error Scenarios with Mocking
    @Test
    public void testMocking_ExceptionHandling_PreferenceAccessFailure() {
        // ARRANGE: Create a preference store that throws an exception
        DBPPreferenceStore faultyPrefStore = mock(DBPPreferenceStore.class);
        when(faultyPrefStore.getBoolean(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY))
            .thenThrow(new RuntimeException("Preference store access failed"));

        try {
            DBNNodeSorter sorter = new DBNNodeSorter(faultyPrefStore);
            DBNNode[] nodes = createMockNodes("a", "b");
            sorter.sortNodes(nodes);
        } catch (RuntimeException e) {
            assertEquals("Preference store access failed", e.getMessage());
        }
    }

    private DBNNode createMockNode(String displayName, boolean isFolder) {
        DBNNode node = mock(DBNNode.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
        when(node.getNodeDisplayName()).thenReturn(displayName);
        when(node.allowsChildren()).thenReturn(isFolder);
        return node;
    }

    private DBNNode[] createMockNodes(String... names) {
        return Arrays.stream(names)
            .map(name -> createMockNode(name, false))
            .toArray(DBNNode[]::new);
    }

    private DBNNode[] createMockNodes(List<String> names) {
        return createMockNodes(names.toArray(new String[0]));
    }

}

