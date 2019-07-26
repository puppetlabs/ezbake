NAMESPACE ?= puppet
git_describe := $(shell git describe)
vcs_ref := $(shell git rev-parse HEAD)
build_date := $(shell date -u +%FT%T)
hadolint_available := $(shell hadolint --help > /dev/null 2>&1; echo $$?)
hadolint_command := hadolint --ignore DL3008 --ignore DL3018 --ignore DL3028 --ignore DL4000 --ignore DL4001
hadolint_container := hadolint/hadolint:latest
pwd := $(shell pwd)
export BUNDLE_PATH = $(pwd)/.bundle/gems
export BUNDLE_BIN = $(pwd)/.bundle/bin
export GEMFILE = $(pwd)/Gemfile

version := $(shell echo $(git_describe) | sed 's/-.*//')
dockerfile := Dockerfile

prep:
	@git fetch --unshallow 2> /dev/null ||:
	@git fetch origin 'refs/tags/*:refs/tags/*'

lint:
ifeq ($(hadolint_available),0)
	@$(hadolint_command) ezbake/$(dockerfile)
else
	@docker pull $(hadolint_container)
	@docker run --rm -v $(PWD)/ezbake/$(dockerfile):/Dockerfile -i $(hadolint_container) $(hadolint_command) Dockerfile
endif

build: prep
	@docker build \
		--pull \
		--build-arg vcs_ref=$(vcs_ref) \
		--build-arg build_date=$(build_date) \
		--build-arg version=$(version) \
		--file ezbake/$(dockerfile) \
		--tag $(NAMESPACE)/ezbake:$(version) $(pwd)/..
ifeq ($(IS_LATEST),true)
	@docker tag $(NAMESPACE)/ezbake:$(version) $(NAMESPACE)/ezbake:latest
endif

test: prep
	@bundle install --path $$BUNDLE_PATH --gemfile $$GEMFILE
	@PUPPET_TEST_DOCKER_IMAGE=$(NAMESPACE)/ezbake:$(version) \
		bundle exec --gemfile $$GEMFILE rspec spec

push-image: prep
	@docker push $(NAMESPACE)/ezbake:$(version)
ifeq ($(IS_LATEST),true)
	@docker push $(NAMESPACE)/ezbake:latest
endif

push-readme:
	@docker pull sheogorath/readme-to-dockerhub
	@docker run --rm \
		-v $(PWD)/README.md:/data/README.md \
		-e DOCKERHUB_USERNAME="$(DISTELLI_DOCKER_USERNAME)" \
		-e DOCKERHUB_PASSWORD="$(DISTELLI_DOCKER_PW)" \
		-e DOCKERHUB_REPO_PREFIX=puppet \
		-e DOCKERHUB_REPO_NAME=ezbake \
		sheogorath/readme-to-dockerhub

publish: push-image push-readme

.PHONY: prep lint build test publish push-image push-readme
