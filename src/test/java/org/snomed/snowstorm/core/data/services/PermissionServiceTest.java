package org.snomed.snowstorm.core.data.services;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.snomed.snowstorm.core.data.domain.security.PermissionRecord;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class PermissionServiceTest {

	private PermissionService permissionService;
	private List<PermissionRecord> allPermissionRecords;
	private List<String> codeSystemBranches;

	@Before
	public void setup() {
		permissionService = new PermissionService();
		allPermissionRecords = new ArrayList<>();

		// Global roles
		allPermissionRecords.add(new PermissionRecord("ADMIN").withUserGroups("snowstorm-admin"));
		allPermissionRecords.add(new PermissionRecord("VIEW").withUserGroups("global-view"));

		// International roles
		allPermissionRecords.add(new PermissionRecord("VIEW", "MAIN").withUserGroups("int-author", "int-reviewer"));
		allPermissionRecords.add(new PermissionRecord("AUTHOR", "MAIN").withUserGroups("int-author", "int-reviewer"));
		allPermissionRecords.add(new PermissionRecord("REVIEW", "MAIN").withUserGroups("int-reviewer"));
		allPermissionRecords.add(new PermissionRecord("AUTHOR", "MAIN/PROJECTA").withUserGroups("int-projectA-author"));

		// Extension code system roles
		allPermissionRecords.add(new PermissionRecord("AUTHOR", "MAIN/SNOMEDCT-ABC").withUserGroups("abc-author"));

		codeSystemBranches = Lists.newArrayList("MAIN", "MAIN/SNOMEDCT-ABC");
	}

	@Test
	public void test() {
		// Test what roles a user has on each branch if they are in the following user groups.

		// Global roles work on any branch or code system
		assertEquals(newHashSet("ADMIN"), getRolesForBranch("MAIN", "ROLE_snowstorm-admin"));
		assertEquals(newHashSet("ADMIN", "VIEW"), getRolesForBranch("MAIN", "ROLE_snowstorm-admin", "ROLE_global-view"));
		assertEquals(newHashSet("ADMIN"), getRolesForBranch("MAIN/PROJECTA", "ROLE_snowstorm-admin"));
		assertEquals(newHashSet("ADMIN"), getRolesForBranch("MAIN/SNOMEDCT-ABC", "ROLE_snowstorm-admin"));
		assertEquals(newHashSet("ADMIN"), getRolesForBranch("MAIN/SNOMEDCT-ABC/something/task-123", "ROLE_snowstorm-admin"));

		// International roles work on MAIN or any sub branch but not other code systems
		assertEquals(newHashSet("VIEW", "AUTHOR"), getRolesForBranch("MAIN", "int-author"));
		assertEquals(newHashSet("VIEW", "AUTHOR", "REVIEW"), getRolesForBranch("MAIN", "int-reviewer"));
		assertEquals(newHashSet("VIEW", "AUTHOR"), getRolesForBranch("MAIN/PROJECTA", "int-author"));
		assertEquals(newHashSet("VIEW", "AUTHOR", "REVIEW"), getRolesForBranch("MAIN/PROJECTA", "int-reviewer"));
		assertEquals(newHashSet(), getRolesForBranch("MAIN/SNOMEDCT-ABC", "int-author"));
		assertEquals(newHashSet(), getRolesForBranch("MAIN/SNOMEDCT-ABC", "int-reviewer"));

		// Roles can be set on a single project
		assertEquals(newHashSet(), getRolesForBranch("MAIN", "int-projectA-author"));
		assertEquals(newHashSet("AUTHOR"), getRolesForBranch("MAIN/PROJECTA", "int-projectA-author"));
		assertEquals(newHashSet("AUTHOR"), getRolesForBranch("MAIN/PROJECTA/task-123", "int-projectA-author"));
		assertEquals(newHashSet(), getRolesForBranch("MAIN/SNOMEDCT-ABC/PROJECTA", "int-projectA-author"));

		// Extension roles
		assertEquals(newHashSet(), getRolesForBranch("MAIN", "abc-author"));
		assertEquals(newHashSet("AUTHOR"), getRolesForBranch("MAIN/SNOMEDCT-ABC", "abc-author"));
		assertEquals(newHashSet("AUTHOR"), getRolesForBranch("MAIN/SNOMEDCT-ABC/PROJECTZ/task-99", "abc-author"));

	}

	private Set<String> getRolesForBranch(String branchPath, String... userGroups) {
		return permissionService.getUserRolesForBranch(branchPath, allPermissionRecords, codeSystemBranches, createUserWithUserGroups(newHashSet(userGroups)));
	}

	private PreAuthenticatedAuthenticationToken createUserWithUserGroups(Set<String> userGroups) {
		return new PreAuthenticatedAuthenticationToken("userA", "123", userGroups.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet()));
	}

}