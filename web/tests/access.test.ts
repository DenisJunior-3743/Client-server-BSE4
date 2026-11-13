import test from 'node:test'
import assert from 'node:assert/strict'
import { canAccessPath, canAccessWorkspace, homeForRole, WORKSPACE_ROLES } from '../src/lib/access.ts'
import { NAV_SECTIONS } from '../src/lib/nav.ts'

for (const [workspace, role] of Object.entries(WORKSPACE_ROLES)) {
  test(`${role} has the expected workspace menu and URL access`, () => {
    const visible = NAV_SECTIONS.filter((section) => canAccessWorkspace(role, section.role))
    assert.equal(visible.length, role === 'ADMIN' ? 4 : 1)
    assert.equal(canAccessPath(role, '/'), role === 'ADMIN')
    for (const [otherWorkspace, otherRole] of Object.entries(WORKSPACE_ROLES)) {
      assert.equal(canAccessPath(role, `/${otherWorkspace}/overview`), role === 'ADMIN' || role === otherRole)
      assert.equal(canAccessPath(role, `/${otherWorkspace}/staff/new`), role === 'ADMIN' || role === otherRole)
    }
    assert.equal(canAccessPath(role, '/account/profile'), true)
    assert.equal(canAccessPath(role, '/applications/example'), true)
    assert.equal(homeForRole(role), role === 'ADMIN' ? '/' : `/${workspace}/overview`)
  })
}

test('borrowers and signed-out users cannot access any staff module', () => {
  for (const role of ['BORROWER', undefined] as const) {
    assert.equal(homeForRole(role), '/login')
    for (const path of ['/', '/admin/staff', '/officer/new', '/analyst/verification', '/manager/approved', '/applications/example']) {
      assert.equal(canAccessPath(role, path), false)
    }
    for (const section of NAV_SECTIONS) assert.equal(canAccessWorkspace(role, section.role), false)
  }
})
