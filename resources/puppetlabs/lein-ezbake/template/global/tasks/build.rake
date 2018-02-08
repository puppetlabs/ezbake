require 'json'

namespace :pl do
  desc "do a local build"
  task :local_build => "pl:fetch" do
    # If we have a dirty source, bail, because changes won't get reflected in
    # the package builds
    Pkg::Util::Version.fail_on_dirty_source

    Pkg::Util::RakeUtils.invoke_task("package:tar")
    # where we want the packages to be copied to for the local build
    nested_output = '../../../output'
    pkg_path = '../pkg'
    staging_path = 'pkg_artifacts'
    FileUtils.cp(Dir.glob("pkg/*.gz").join(''), FileUtils.pwd)
    # unpack the tarball we made during the build step
    stdout, stderr, exitstatus = Pkg::Util::Execution.capture3(%(tar xf #{Dir.glob("*.gz").join('')}))
    Pkg::Util::Execution.success?(exitstatus) or raise "Error unpacking tarball: #{stderr}"
    Dir.chdir("#{Pkg::Config.project}-#{Pkg::Config.version}") do
      Pkg::Config.final_mocks.split(" ").each do |mock|
        platform = mock.split('-')[1..-2].join('-')
        platform_path = platform.gsub(/\-/, '')
        os, ver = /([a-zA-Z]+)(\d+)/.match(platform_path).captures
        puts "===================================="
        puts "Packaging for #{os} #{ver}"
        puts "===================================="
        stdout, stderr, exitstatus = Pkg::Util::Execution.capture3(%(bash controller.sh #{os} #{ver} #{staging_path}))
        Pkg::Util::Execution.success?(exitstatus) or raise "Error running packaging: #{stdout}\n#{stderr}"
        puts "#{stdout}\n#{stderr}"

        # I'm so sorry
        # These paths are hard-coded in packaging, so hard code here too.
        # When everything is moved to artifactory this should be able
        # to be fixed. --MMR, 2017-08-30
        if Pkg::Config.build_pe
          platform_path = "pe/rpm/#{os}-#{ver}-"
        else
          # carry forward defaults from mock.rake
          repo = Pkg::Config.yum_repo_name || 'products'
          platform_path = "#{os}/#{ver}/#{repo}/"
        end

        # We want to include the arches for el/sles/fedora paths
        ['x86_64', 'i386'].each do |arch|
          target_dir = "#{pkg_path}/#{platform_path}#{arch}"
          FileUtils.mkdir_p(target_dir) unless File.directory?(target_dir)
          FileUtils.cp(Dir.glob("*#{os}#{ver}*.rpm"), target_dir)
        end
      end
      Pkg::Config.cows.split(" ").each do |cow|
        # So you might think, from looking at
        # https://github.com/puppetlabs/packaging/blob/551be049ae0261f0dd1b632993d4fbe1ada63d9c/lib/packaging/deb/repo.rb#L72
        # that we want the repo to default to 'main' if unset. However, looking
        # deeper into that method we need repo to be '' if apt_repo_name is unset
        # https://github.com/puppetlabs/packaging/blob/551be049ae0261f0dd1b632993d4fbe1ada63d9c/lib/packaging/deb/repo.rb#L106
        repo = Pkg::Config.apt_repo_name || ''
        platform = cow.split('-')[1..-2].join('-')

        # Keep on keepin' on with hardcoded paths in packaging
        # Hopefully this goes away with artifactory.
        #  --MMR, 2017-08-30
        platform_path = "pe/deb/#{platform}"
        unless Pkg::Config.build_pe
          # get rid of the trailing slash if repo = ''
          platform_path = "deb/#{platform}/#{repo}".sub(/\/$/, '')
        end

        FileUtils.mkdir_p("#{pkg_path}/#{platform_path}") unless File.directory?("#{pkg_path}/#{platform_path}")
        # there's no differences in packaging for deb vs ubuntu so picking debian
        # if that changes we'll need to fix that
        puts "===================================="
        puts "Packaging for #{platform}"
        puts "===================================="
        stdout, stderr, exitstatus = Pkg::Util::Execution.capture3(%(bash controller.sh debian #{platform} #{staging_path}))
        Pkg::Util::Execution.success?(exitstatus) or raise "Error running packaging: #{stdout}\n#{stderr}"
        puts "#{stdout}\n#{stderr}"
        FileUtils.cp(Dir.glob("*#{platform}*.deb"), "#{pkg_path}/#{platform_path}")
      end
      FileUtils.cp_r(pkg_path, nested_output)
      FileUtils.rm_r(staging_path)
    end
  end

  desc "get the property and bundle artifacts ready"
  task :prep_artifacts, [:output_dir] => "pl:fetch" do |t, args|
    props = Pkg::Config.config_to_yaml
    bundle = Pkg::Util::Git.git_bundle('HEAD')
    FileUtils.cp(props, "#{args[:output_dir]}/BUILD_PROPERTIES")
    FileUtils.cp(bundle, "#{args[:output_dir]}/PROJECT_BUNDLE")
  end

  namespace :jenkins do
    desc "trigger jenkins packaging job"
    task :trigger_build, [:auth_string, :job_url] do |t, args|
      Pkg::Util::RakeUtils.invoke_task("pl:prep_artifacts", "#{Dir.pwd}")

      curl_opts = [
        '--request POST',
        "--form file0=@#{Dir.pwd}/BUILD_PROPERTIES",
        "--form file1=@#{Dir.pwd}/PROJECT_BUNDLE",
      ]

      platform_version = ENV['PLATFORM_VERSION']
      platform_version ||= 'PC1'

      parameter_json = {
        :parameter => [
          {
            :name => "BUILD_PROPERTIES",
            :file => "file0"
          },
          {
            :name => "PROJECT_BUNDLE",
            :file => "file1"
          },
          {
            :name => "COWS",
            :value => "#{Pkg::Config.cows}"
          },
          {
            :name => "MOCKS",
            :value => "#{Pkg::Config.final_mocks}"
          },
          {
            :name => "PLATFORM_VERSION",
            :value => platform_version
          }
        ]
      }

      if Pkg::Config.build_pe
        Pkg::Util.check_var('PE_VER', ENV['PE_VER'])
        parameter_json[:parameter] << { :name => "PE_VER", :value => "#{ENV['PE_VER']}" }
      end

      curl_opts << %(--form json='#{parameter_json.to_json}')
      if args[:auth_string] =~ /:/
        curl_opts << "--user #{args[:auth_string]}"
        Pkg::Util::Net.curl_form_data("#{args[:job_url]}/build", curl_opts)
      #assume we're using a token
      else
        Pkg::Util::Net.curl_form_data("#{args[:job_url]}/build?token=#{args[:auth_string]}", curl_opts)
      end
      Pkg::Util::Net.print_url_info(args[:job_url])
      package_url = "#{Pkg::Config.builds_server}/#{Pkg::Config.project}/#{Pkg::Config.ref}"
      puts "After the build job is completed, packages will be available at:"
      puts package_url
    end

    desc "trigger jenkins packaging job with local auth"
    task :trigger_build_local_auth => "pl:fetch" do
      if Pkg::Config.build_pe
        jenkins = 'cinext-jenkinsmaster-enterprise-prod-1'
        stream = 'enterprise'
      else
        jenkins = 'jenkins-master-prod-1'
        stream = 'platform'
      end
      job_url = "https://#{jenkins}.delivery.puppetlabs.net/job/#{stream}_various-packaging-jobs_packaging-os-clj_lein-ezbake-generic"

      begin
        auth = Pkg::Util.check_var('JENKINS_USER_AUTH', ENV['JENKINS_USER_AUTH'])
        Pkg::Util::RakeUtils.invoke_task("pl:jenkins:trigger_build", auth, job_url)
      rescue
        STDERR.puts "You need to pass the environment variable JENKINS_USER_AUTH"
        STDERR.puts "It should be in the format <LDAP username>:<access token>"
        STDERR.puts "To find your access token, go to http://<jenkins-url>/me/configure"
        STDERR.puts "These jobs also are configured with an authentication token"
        STDERR.puts "that you can use instead of your personal token. To find the"
        STDERR.puts "job authentication token see the 'Build Triggers' section of"
        STDERR.puts "#{job_url}/configure. In this case, JENKINS_USER_AUTH should"
        STDERR.puts "be set to only the Authentication Token."
      end
    end
  end
end
