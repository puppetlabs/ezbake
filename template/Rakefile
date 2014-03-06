require 'rake'

JAR_FILE = 'classifier.jar'

RAKE_ROOT = File.dirname(__FILE__)

#LEIN_SNAPSHOTS_IN_RELEASE = 'y'

# Load tasks and variables for packaging automation
begin
  load File.join(RAKE_ROOT, 'ext', 'packaging', 'packaging.rake')
rescue LoadError
end

# We want to use classifier's package:tar and its dependencies, because it
# contains all the special java snowflake magicks, so we have to clear the
# packaging repo's. We also want to use classifier's clean task, since it has so
# much more clean than the packaging repo knows about
['clean'].each do |task|
  Rake::Task[task].clear if Rake::Task.task_defined?(task)
end

# All variables have been set, so we can load the classifier tasks
Dir[ File.join(RAKE_ROOT, 'tasks','*.rake') ].sort.each { |t| load t }

task :default => [ :package ]

task :allclean => [ :clobber ]

desc "Remove build artifacts (other than clojure (lein) builds)"
task :clean do
  rm_rf FileList["ext/files", "pkg", "*.tar.gz"]
end

desc "Get rid of build artifacts including clojure (lein) builds"
task :clobber => [ :clean ] do
  rm_rf FileList["target/classifier*jar"]
end

if defined?(Pkg) and defined?(Pkg::Config)
  @version = Pkg::Config.version
else
  begin
    %x{which git >/dev/null 2>&1}
    if $?.success?
      @version = %x{git describe --always --dirty}
      if $?.success?
        @version.chomp!
      end
    end
  rescue
    @version = "0.0-dev-build"
  end
end

task :version do
  puts @version
end

desc 'Build deb package'
task :deb => [ 'package:implode', 'package:bootstrap', 'package:deb' ]

desc 'Build a Source rpm for classifier'
task :srpm => [ 'package:implode', 'package:bootstrap', 'package:srpm' ]

