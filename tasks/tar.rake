# JAR_FILE the constant is defined in Rakefile
#
file JAR_FILE do |t|
  Rake::Task[:uberjar].invoke
end

task :"package:tar" => JAR_FILE
