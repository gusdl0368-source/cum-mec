# gstack

Use gstack skills for the workflows below. **All web browsing must go through gstack's `/browse` skill.** Do **not** use `mcp__claude-in-chrome__*` tools under any circumstances — they are forbidden.

## Web browsing rule

- For any task that requires opening a URL, navigating a page, interacting with web UI, taking screenshots, dogfooding a deploy, verifying a site, or filing a bug with web evidence: invoke `/browse`.
- Never call `mcp__claude-in-chrome__*` tools. If you find yourself reaching for them, stop and use `/browse` instead.

## Available gstack skills

Planning & review:
- `/office-hours` — pair with a senior reviewer-style agent on open work
- `/plan-ceo-review` — CEO-level review of a plan
- `/plan-eng-review` — engineering review of a plan
- `/plan-design-review` — design review of a plan
- `/plan-devex-review` — DevEx review of a plan
- `/design-consult` — design consultation on a problem
- `/design-review` — review a design / UI change
- `/design-shotgun` — generate many design directions fast
- `/design-html` — produce HTML design mocks
- `/devex-review` — DevEx review of changes
- `/review` — review a pull request / branch
- `/cso` — chief security officer review
- `/autoplan` — produce a plan automatically
- `/investigate` — investigate a bug / incident / question
- `/retro` — run a retrospective
- `/learn` — capture learnings into memory

Shipping & ops:
- `/ship` — ship the current branch
- `/land-and-deploy` — land and deploy
- `/canary` — run a canary deploy
- `/qa` — full QA pass
- `/qa-only` — QA without other ship steps
- `/document-release` — write release docs
- `/freeze` — freeze production
- `/unfreeze` — unfreeze production
- `/guard` — guard a critical operation
- `/careful` — extra-careful execution mode
- `/benchmark` — run benchmarks

Browsing & setup:
- `/browse` — headless browser for QA, dogfooding, screenshots (USE THIS for all web browsing)
- `/connect-chrome` — connect to a Chrome instance
- `/setup-browser-cookies` — set up browser cookies for authenticated browsing
- `/setup-deploy` — set up deployment
- `/setup-gbrain` — set up gbrain
- `/gstack-upgrade` — upgrade gstack itself
- `/codex` — use codex helper
