# illumos' Continuous Integration Repository

## Overview of Repository Directory Structure

### "ansible" Directory

The "ansible" directory contains the Ansible configuration roles and
playbooks used to configure a new CI server capable of hosting the
Jenkins master service. This role has only been tested using an Ubuntu
16.04 based VM running in Joyent's cloud, so any other environment may
not work "out of the box".

Additionally, this directory also holds the Ansible configuration that
is used to configure the dynamic build/test Jenkins slaves.

### "docker" Directory

The "docker" directory holds all of the configure that is needed to
build the Docker image that is used by the Jenkins master service. We
run the Jenkins service in a Docker container, and the files contained
in this directory are used to build a custom Docker image for this
container (which is based on the "official" Jenkins/Docker image).

### "jenkins" Directory

The "jenkins" directory contains the files used to implement the Jenkins
job that performs the automated builds and testing. Each subdirectory
underneath "jenkins" has a designated purpose:

  - "jobs" - The files in this directory are parsed by the "seed" job of
    the Jenkins master, populating the Jenkins jobs on the master from
    the groovy source files found here. The files in this directory are
    interpreted by the "Jobs DSL" plugin, and should conform to the
    syntax it requires, and feature set it provides.

  - "pipelines" - Unlike the files in the "jobs" directory, the files in
    "pipelines" do not necessary correspond directly to Jenkins jobs.
    Instead, this directory is intended to hold files that will be
    loaded by the Jenkins jobs (i.e. the files in the "jobs" directory)
    and used to provided the implementation logic of a "Pipeline Job".

  - "sh" - This directory contains various bash scripts that are
    executed in the context of a Jenkins job.

### "scripts" Directory

The "scripts" directory contains miscellaneous scripts that are useful,
but not intended to be run by any automation. Instead, these scripts
provide a more convienient way to execute certain commands (or sets of
commands) that would otherwise have to be manually run (and remembered).

## Overview of the "regression-tests" Jenkins Job

The "regression-tests" Jenkins job provides the logic that orchestrates
all of the different steps that are needed to _minimally_ test illumos
commits. This job will perform the following sequence of steps:

  1. Create OpenIndiana based VM in Amazon EC2.
  2. Checkout illumos code to be built, installed, and tested.
  3. Full nightly build of code checked out in (2), using VM created in
     (1). This includes "debug", "nondebug", and "lint" builds.
  4. Upgrade VM using "debug" packages built in (3).
  5. Create multiple clones of upgraded VM generated in (4).
  6. Use clones generated in (5) to run the following tests:
    - "libc-tests"
    - "os-tests"
    - "util-tests"
    - "zfs-tests"
    - "zloop"
  7. Archive the test results of tests run in (6).

## How To Configure the CI Server Using an Ubuntu in Joyent's Cloud

  1. Create Ubuntu 16.04 VM using Joyent's UI.
  2. Log in to VM using `ssh` and the `ubuntu` user.
    - All following steps must be executed on the VM itself
  3. Use Ansible to configure the VM by running these commands:
    1. `git clone https://github.com/prakashsurya/illumos-ci`
    2. `cd illumos-ci`
    3. `sudo ./scripts/ubuntu/install-ansible-dependencies.sh`
    4. `./scripts/ansible/apply-playbook.sh`
  4. Start the Vault service by running these commands:
    1. `sudo systemctl start vault`
  5. Configure `.bashrc` of the `ubuntu` user for Vault access
    - `echo "export VAULT_ADDR=http://127.0.0.1:8200" >> ~/.bashrc`
    - `echo "export PATH=$PATH:/mnt/vault-0.6.1" >> ~/.bashrc`
    - log out and log back in, so these evironment variables take effect
  6. Initialize and populate the Vault secrets datastore
    - This process is not yet documented.
  7. Initialize the Jenkins service
    - This process is not yet documented.
