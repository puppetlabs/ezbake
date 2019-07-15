require 'rspec/core'
require 'fileutils'
require 'open3'
require 'pupperware/spec_helper'

SPEC_DIRECTORY = File.dirname(__FILE__)
DOCKER_DIRECTORY = File.dirname(SPEC_DIRECTORY)
EZBAKE_DIRECTORY = File.dirname(DOCKER_DIRECTORY)

describe 'ezbake_container' do
  include Pupperware::SpecHelpers

  def run_build(repo, branch)
    run_command("docker run --detach \
                   --env PROJECT_REPO=#{repo} \
                   --env PROJECT_BRANCH=#{branch} \
                   --env UPDATE_EZBAKE_VERSION=true \
                   --env EZBAKE_VERSION=#{@ezbake_version} \
                   #{@image}")
  end

  before(:all) do
    @image = require_test_image
    # the version in ezbake's project.clj looks like
    #    (defproject puppetlabs/lein-ezbake "2.0.4-SNAPSHOT"
    # Look for that line and parse out the version string for use in the builds
    @ezbake_version = File.open(File.join(EZBAKE_DIRECTORY, "project.clj")) do |f|
      /.* "(.*)"/.match(f.grep(/defproject/).first.chomp)[1]
    end
  end

  it 'should be able to build puppetserver' do
    result = run_build('https://github.com/puppetlabs/puppetserver', 'master')
    container = result[:stdout].chomp
    wait_on_container_exit(container, 450)
    expect(get_container_exit_code(container)).to eq(0)
    emit_log(container)
    teardown_container(container)
  end

  it 'should be able to build puppetdb' do
    result = run_build('https://github.com/puppetlabs/puppetdb', 'master')
    container = result[:stdout].chomp
    wait_on_container_exit(container, 450)
    expect(get_container_exit_code(container)).to eq(0)
    emit_log(container)
    teardown_container(container)
 end
end
