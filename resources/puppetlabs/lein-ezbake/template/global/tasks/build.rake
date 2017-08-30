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
    FileUtils.mv(Dir.glob("pkg/*.gz").join(''), FileUtils.pwd)
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
        stdout, stderr, exitstatus = Pkg::Util::Execution.capture3(%(bash controller.sh #{os} #{ver}))
        Pkg::Util::Execution.success?(exitstatus) or raise "Error running packaging: #{stdout}\n#{stderr}"
        puts "#{stdout}\n#{stderr}"

        # I'm so sorry
        # These paths are hard-coded in packaging, so hard code here too.
        # When everything is moved to packaging 1.0.x this should be able
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
        # carry forward defaults from Packaging::Deb::Repo
        repo = Pkg::Config.apt_repo_name || 'main'
        platform = cow.split('-')[1..-2].join('-')

        # Keep on keepin' on with hardcoded paths in packaging
        # Hopefully this goes away with packaging 1.0.x.
        #  --MMR, 2017-08-30
        if Pkg::Config.build_pe
          platform_path = "pe/deb/#{platform}"
        else
          platform_path = "deb/#{platform}/#{repo}"
        end

        FileUtils.mkdir_p("#{pkg_path}/#{platform_path}") unless File.directory?("#{pkg_path}/#{platform_path}")
        # there's no differences in packaging for deb vs ubuntu so picking debian
        # if that changes we'll need to fix that
        puts "===================================="
        puts "Packaging for #{platform}"
        puts "===================================="
        stdout, stderr, exitstatus = Pkg::Util::Execution.capture3(%(bash controller.sh debian #{platform}))
        Pkg::Util::Execution.success?(exitstatus) or raise "Error running packaging: #{stdout}\n#{stderr}"
        puts "#{stdout}\n#{stderr}"
        FileUtils.cp(Dir.glob("*#{platform}*.deb"), "#{pkg_path}/#{platform_path}")
      end
      FileUtils.cp_r(pkg_path, nested_output)
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
      if Pkg::Config.build_pe
        Pkg::Util.check_var('PE_VER', ENV['PE_VER'])
        curl_opts << %(--form json='{"parameter": [{"name":"PE_VER", "value":"#{ENV['PE_VER']}"},{"name":"BUILD_PROPERTIES", "file":"file0"},{"name":"PROJECT_BUNDLE", "file":"file1"}]}')
      else
        curl_opts << %(--form json='{"parameter": [{"name":"BUILD_PROPERTIES", "file":"file0"},{"name":"PROJECT_BUNDLE", "file":"file1"}]}')
      end
      if args[:auth_string] =~ /:/
        curl_opts << "--user #{args[:auth_string]}"
        Pkg::Util::Net.curl_form_data("#{args[:job_url]}/build", curl_opts)
      #assume we're using a token
      else
        Pkg::Util::Net.curl_form_data("#{args[:job_url]}/build?token=#{args[:auth_string]}", curl_opts)
      end
      Pkg::Util::Net.print_url_info(args[:job_url])
    end
  end
end
