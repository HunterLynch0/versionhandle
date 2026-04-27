# VersionHandle (vh)

A lightweight version control system built from scratch (inspired by git).

## Features

* `init` – initialise a repository
* `add` – stage files
* `commit` – create commits
* `status` – view changes
* `log` – view commit history
* `branch` – create branches
* `checkout` – switch commits/branches
* `merge` – merge branches

---

## Installation

### One-line install

```bash
curl -L https://github.com/YOUR_USERNAME/versionhandle/releases/download/v1.0.0/install.sh | bash
```

Then restart your terminal or run:

```bash
source ~/.zshrc
```

---

## Usage

```bash
vh init
vh add .
vh commit "first commit"
vh status
vh log
```

---


## Example

```bash
vh init
vh add file.txt
vh commit "added file"
vh branch feature
vh checkout feature
```

---

## How it works

VersionHandle stores:

* objects (file contents hashed with SHA-256)
* commits (snapshots + metadata)
* branches (pointers to commits)

Everything is stored inside:

```
.versionhandle/
```

---

## Built With

* Java 17
* Maven

---

## Notes

This project was built to gain a better understanding of Git and how it works internally.

---

## License

MIT
