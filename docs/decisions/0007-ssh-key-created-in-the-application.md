---
status: accepted
date: 2026-11-12
decision-makers: Oliver Kopp
---

# SSH Key Created in the Application on Devices without SSH Configuration

`adr~ssh-key-in-app~1`

Needs: dsn

## Context and Problem Statement

Git synchronization over SSH needs a private key and a way to verify the server.
A desktop has both in `~/.ssh`; a phone has neither.
Where do the key and the trusted host keys come from on a phone?

## Considered Options

* The application creates a key pair and shows the public key
* The user pastes an existing private key into the settings
* HTTPS with a personal access token

## Decision Outcome

Chosen option: "The application creates a key pair and shows the public key", because the private key never leaves the device and the user can register the public key as a deploy key that grants access to the one repository only.

The key is an ECDSA key on the curve NIST P-256.
All supported Android versions create such a key with the platform's own cryptography, while Ed25519 would need an additional library on Android versions before 13.

The key of a server is trusted on the first connection and recorded; a different key on a later connection is rejected.

### Consequences

* Good, because losing the phone compromises one deploy key, which is revoked at the git server, and no key that is used elsewhere.
* Bad, because the first connection to a server is not protected against an attacker in the network path.
* Bad, because the private key is stored unencrypted in the private storage of the application; it can be read on a rooted device.
* Neutral, because each device needs its own key registration.

## Pros and Cons of the Options

### The user pastes an existing private key into the settings

* Good, because no registration at the git server is needed.
* Bad, because a private key is copied between devices, typically through the clipboard or a messenger.

### HTTPS with a personal access token

* Good, because it works where SSH is blocked.
* Bad, because a token usually grants access to more than one repository, and it expires.
