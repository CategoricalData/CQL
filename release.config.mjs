// semantic-release configuration, used by the "release" job in .github/workflows/ci.yml. Every
// push to master that passes CI is released: semantic-release computes the next version from the
// commits since the last v* tag, builds the runnable jar with that version, tags the commit, and
// publishes a GitHub release with the jar attached as cql.jar.
//
// Each commit is classified on its own, and the highest bump in the range wins:
//   - breaking change ("feat!: ..." or a "BREAKING CHANGE:" footer)  -> major
//   - "feat: ..." or "feature: ..."                                    -> minor
//   - anything else, including messages that are not conventional      -> patch
//
// The release notes list every commit except merge commits. Commits that are not conventional
// are listed under "Other Changes" with the first line of their message.

/**
 * Conventional commit types, in the order their sections appear in the release notes. A header
 * counts as conventional only when it starts with one of these lowercase types, so a free-form
 * message such as "Experimental: centering of values" is not read as type "Experimental", which
 * has no section and would drop out of the notes.
 */
const COMMIT_TYPES = [
  { type: 'feat', section: 'Features' },
  { type: 'feature', section: 'Features' },
  { type: 'fix', section: 'Bug Fixes' },
  { type: 'perf', section: 'Performance Improvements' },
  { type: 'revert', section: 'Reverts' },
  { type: 'refactor', section: 'Code Refactoring' },
  { type: 'docs', section: 'Documentation' },
  { type: 'test', section: 'Tests' },
  { type: 'build', section: 'Build System' },
  { type: 'ci', section: 'Continuous Integration' },
  { type: 'style', section: 'Styles' },
  { type: 'chore', section: 'Miscellaneous Chores' },
  // A header that does not parse has no type, which the preset looks up as the empty type.
  { type: '', section: 'Other Changes' },
];

const typeAlternation = COMMIT_TYPES.map(({ type }) => type)
  .filter((type) => type !== '')
  .join('|');

/** Parser options, shared by the commit analyzer and the release notes generator. */
const parserOpts = {
  // type(scope)!: subject, with the scope and the "!" optional.
  headerPattern: new RegExp(`^(${typeAlternation})(?:\\((.*)\\))?!?: (.*)$`),
  // The same header with the "!" required, which marks a breaking change.
  breakingHeaderPattern: new RegExp(`^(${typeAlternation})(?:\\((.*)\\))?!: (.*)$`),
};

/** Options for the conventionalcommits preset. */
const presetConfig = {
  types: COMMIT_TYPES,
  // Merge messages written by git and GitHub. The commits that a merge brings in are listed on
  // their own, so listing the merge as well would repeat them.
  ignoreCommits: /^Merge (pull request #\d+ from|branch '|remote-tracking branch '|tag '|commit ')/,
};

/** @type {import('semantic-release').GlobalConfig} */
export default {
  branches: ['master'],
  tagFormat: 'v${version}',
  plugins: [
    [
      '@semantic-release/commit-analyzer',
      {
        preset: 'conventionalcommits',
        presetConfig,
        parserOpts,
        // The analyzer applies the highest release among the rules that match a commit. The last
        // rule matches every commit, so each commit is at least a patch, and the analyzer's
        // default rules (which release nothing for docs, chore, ci, ... or free-form messages)
        // are never consulted.
        releaseRules: [
          { breaking: true, release: 'major' },
          { type: 'feat', release: 'minor' },
          { type: 'feature', release: 'minor' },
          { release: 'patch' },
        ],
      },
    ],
    [
      '@semantic-release/release-notes-generator',
      {
        preset: 'conventionalcommits',
        presetConfig,
        parserOpts,
      },
    ],
    [
      '@semantic-release/exec',
      {
        prepareCmd: './gradlew shadowJar -PreleaseVersion=${nextRelease.version}',
      },
    ],
    [
      '@semantic-release/github',
      {
        // Earlier releases published the jar as cql.jar, so links to
        // releases/latest/download/cql.jar keep working.
        assets: [{ path: 'build/libs/CQL-*-all.jar', name: 'cql.jar' }],
        // No comments or labels on the released issues and pull requests, and no issue opened
        // when a release fails (the failed CI run already reports it). This keeps the job's
        // token at contents: write.
        successCommentCondition: false,
        failCommentCondition: false,
      },
    ],
  ],
};
