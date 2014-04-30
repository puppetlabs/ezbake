# Task for building the jar file containing all the things

project_data_file = File.join(RAKE_ROOT, 'ext', 'project_data.yaml')

if File.exist?(project_data_file)
  begin
    require 'yaml'
    @project_data ||= YAML.load_file(project_data_file)
  rescue Exception => e
    STDERR.puts "Unable to load yaml from #{project_data_file}:"
    raise e
  end
end

desc "Build the uberjar"
task :uberjar => [  ] do
  @nexus_version = @project_data['nexus_version']
  if `which lein`
    sh "export NEXUS_VERSION=#{@nexus_version}; lein -U uberjar"
    mv "target/#{EZBake::Config[:uberjar_name]}", EZBake::Config[:uberjar_name]
  else
    puts "You need lein on your system"
    exit 1
  end
end

