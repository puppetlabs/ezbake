# [puppetlabs/ezbake](https://github.com/puppetlabs/ezbake)

The Dockerfile for this image is available in the ezbake repository [here][1].

This container will let you build custom packages for projects using ezbake,
including PuppetServer and PuppetDB.

## Configuration

The following environment variables are supported:

- `EZBAKE_ALLOW_UNREPRODUCIBLE_BUILDS`

  Allow ezbake to use locally-hosted SNAPSHOT builds rather than requiring
  they come from a configured snapshots repository. Defaults to `true`

- `EZBAKE_NODEPLOY`

  Do not deploy SNAPSHOT builds to a configured snapshots repository. Defaults
  to `true`

- `GEM_SOURCE`

  The rubygems host you want to point to. Defaults to `https://rubygems.org`

- `LEIN_PROFILES`

  The lein profiles to use to build your package. Use a comma-separated list for multiple
  profiles. Defaults to `ezbake`

- `EZBAKE_REPO`

  The URL for the repo of your custom ezbake. i.e., `https://github.com/puppetlabs/ezbake`.
  Unset by default

- `EZBAKE_BRANCH`

  The branch you want to build your custom ezbake from. Unset by default. You should only
  set one of `EZBAKE_BRANCH` and `EZBAKE_REF`

- `EZBAKE_REF`

  The git ref you want to build your custom ezbake from. Unset by default. You should only
  set one of `EZBAKE_BRANCH` and `EZBAKE_REF`

- `PROJECT_REPO`

  The URL for the repo you're building. i.e., `https://github.com/underscorgan/puppetdb`.
  Unset by default

- `PROJECT_BRANCH`

  The branch you want to build your packages from. Unset by default. You should only set
  one of `PROJECT_BRANCH` and `PROJECT_REF`

- `PROJECT_REF`

  The git ref you want to build your packages from. Unset by default. You should only set
  one of `PROJECT_BRANCH` and `PROJECT_REF`

- `MOCK`

  *NOTE* `MOCK` and `COW` have some weird requirements tied to assumptions in the [packaging repo](https://github.com/puppetlabs/packaging). There's a plan to make this better at some point.

  The rpm platforms you want to build. Individual entries must match the pattern
  `pl-<os>-<version>-<arch>`. For multiple OSes, use a space separated list. Defaults to
  `pl-el-7-x86_64 pl-el-8-x86_64 pl-redhatfips-7-x86_64 pl-sles-12-x86_64`

- `COW`

  *NOTE* `MOCK` and `COW` have some weird requirements tied to assumptions in the [packaging repo](https://github.com/puppetlabs/packaging). There's a plan to make this better at some point.

  The deb platforms you want to build. Individual entries must match the pattern
  `base-<codename>-<arch>.cow`. For multiple OSes, use a space separated list. Defaults to 
  `base-bionic-i386.cow base-buster-i386.cow base-focal-i386.cow base-bullseye-i386.cow`

- `UPDATE_EZBAKE_VERSION`

  Whether or not to update the ezbake version in the project you're building. This is
  mostly intended to let you test ezbake changes without having to commit code with a `-SNAPSHOT`
  version in project.clj. Defaults to false

- `EZBAKE_VERSION`

  The version of ezbake to use for your build. Only does anything if `UPDATE_EZBAKE_VERSION`
  is set to true. Defaults to unset


## Running

There are a few different ways I've found for running builds on this container. I'll document
the pros and cons here.

This container assumes that the code you're building will be in `/workspace`. The packages
will be copied into `/output` before the container terminates, and if you're building a
custom ezbake that will be cloned into `/ezbake`.

1. Run from a remotely accessible repo (github, gitlab, etc)

This is by far the fastest build option, but does require the extra step of committing and pushing your code
rather than using the code in your working dir.

```
docker run --rm --volume $(PWD)/output:/output --env PROJECT_REPO=https://github.com/underscorgan/puppetdb --env PROJECT_BRANCH=my_feature_branch puppet/ezbake
```

2. Mount in your current working directory

    * Please note: due to the amount of I/O used in compiling the artifacts it will be _incredibly_ slow if you
mount in your entire current working directory. The options below will speed it up significantly (I haven't
actually managed to get a build to finish and have killed it after ~30 minutes when I've mounted in my whole
working directory), but it still takes significantly more than the builds from remote repo. The options are:

    a. Mount in full working directory and use anonymous volumes for the I/O heavy directories

    This is the slower of the two options, but also the simplest. The volumes in the example should be the
    same for all ezbake projects.

    ```
    docker run --rm --volume $(PWD)/output:/output --volume /workspace/test --volume /workspace/test-resources --volume /workspace/target --volume /workspace/tmp puppet/ezbake
    ```
    
    b. Only mount in needed files/directories

    This is faster than mounting in everything and using anonymous volumes, but it'll take more effort
    to get the correct things mounted in. This will vary some between projects, but you can get a pretty
    good idea of what is needed by looking at the `.gitignore` or `.dockerignore` files.

    *NOTE* you will always need to mount in `.git` so the packaging can figure out what version it's supposed
    to be (uses `git describe`).

    ```
    docker run --rm --volume $(PWD)/output:/output --volume $(PWD)/src:/workspace/src --volume $(PWD)/project.clj:/workspace/project.clj --volume $(PWD)/resources:/workspace/resources --volume $(PWD)/.git:/workspace/.git puppet/ezbake
    ```

[1]: https://github.com/puppetlabs/ezbake/blob/master/docker/ezbake/Dockerfile
