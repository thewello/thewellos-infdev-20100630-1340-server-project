# Security

This project is a hobby project built with AI-assisted "vibe coding". It has not been independently audited.
Run a public server only if you understand the risks below.

## Reporting a vulnerability

Please do not open a public issue for a security problem. Use GitHub's private reporting instead: go to the
**Security** tab of this repository and choose **Report a vulnerability**.

## Design notes and known limits

- **Account verification.** With `online-mode=true` (the default), each player proves ownership of a Mojang account
  through Mojang's session server before joining. With `online-mode=false`, no verification happens and anyone can
  join with any name, including an operator's name. Use `online-mode=false` only on a private network.
- **Tokens.** The client sends the account access token only to Mojang's and Microsoft's official addresses. It is
  never sent over the game protocol.
- **Traffic is not encrypted.** The game protocol is plain TCP. Player names, chat and positions can be read by anyone
  on the network path. Do not rely on it for anything private.
- **Plugins run with full server privileges.** A plugin can do anything the server can do. Install only plugins you
  trust, and read their source first.
- **Operators.** Operators can run every command, including `stop`, `op` and `ban`. Keep the operator list short.
- **Input handling.** Player names are limited to letters, digits and underscores (2-16 characters). Packet sizes are bounded.
  Strings are read with the protocol's 64 KB limit and then checked against per-field maximums. Chat is sanitised
  before it is written to the log.
- **Port mapping.** Automatic port mapping (UPnP) only trusts a router that answered the discovery request from the
  same address, and only sends requests to that router.

## Supported versions

Only the latest release receives fixes.
