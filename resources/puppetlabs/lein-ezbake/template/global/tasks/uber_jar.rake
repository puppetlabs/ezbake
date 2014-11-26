# Task for building the jar file containing all the things

desc "Build the uberjar"
task :uberjar => [  ] do
  if `which lein`
    sh "lein -U with-profile ezbake uberjar"
    mv "target/#{EZBake::Config[:uberjar_name]}", EZBake::Config[:uberjar_name]
  else
    puts "You need lein on your system"
    exit 1
  end
end

