namespace :pl do
  desc "do a local build"
  task :local_build => "pl:fetch" do
    # If we have a dirty source, bail, because changes won't get reflected in
    # the package builds
    Pkg::Util::Version.fail_on_dirty_source

    Pkg::Util::RakeUtils.invoke_task("package:tar")
    # at this point we're in a directory like puppetserver/target/staging
    # and we want the output to be under puppetserver
    base_output = '../../output'
    # we've got two chdirs before we actually build the packages, set up
    # this variable so we can copy things more easily
    nested_output = '../../../../output'
    FileUtils.mkdir(base_output) unless File.directory?(base_output)
    Dir.chdir('pkg') do
      # unpack the tarball we made during the build step
      `tar xf #{Dir.glob("*.gz").join('')}`
      Dir.chdir("#{Pkg::Config.project}-#{Pkg::Config.version}") do
        Pkg::Config.final_mocks.split(" ").each do |mock|
          platform = mock.split('-')[1..-2].join('-')
          platform_path = platform.gsub(/\-/, '')
          os, ver = /([a-zA-Z]+)(\d+)/.match(platform_path).captures
          platform_path = "#{os}/#{ver}"
          FileUtils.mkdir_p("#{nested_output}/#{platform_path}") unless File.directory?("#{nested_output}/#{platform_path}")
          `bash controller.sh #{platform}`
          FileUtils.cp(Dir.glob("*#{os}#{ver}*.rpm"), "#{nested_output}/#{platform_path}")
        end
        Pkg::Config.cows.split(" ").each do |cow|
          platform = cow.split('-')[1..-2].join('-')
          platform_path = "deb/#{platform}"
          FileUtils.mkdir_p("#{nested_output}/#{platform_path}") unless File.directory?("#{nested_output}/#{platform_path}")
          `bash controller.sh #{cow}`
          FileUtils.cp(Dir.glob("*#{platform}*.deb"), "#{nested_output}/#{platform_path}")
        end
      end
    end
  end
end
