namespace :test do
  desc "Run integration tests using beaker"
  task :integration, :test_files do |t, args|
    args.with_defaults(:test_files => 'integration/tests/')
    config = ENV["BEAKER_CONFIG"] || "vbox-el6-64mda"
    preserve_hosts = ENV["BEAKER_PRESERVE_HOSTS"] == "true" ? true : false
    color = ENV["BEAKER_COLOR"] == "true" ? true : false
    xml = ENV["BEAKER_XML"] == "true" ? true : false
    type = ENV["BEAKER_TYPE"] || "git"

    beaker = "beaker " +
       "-c '#{RAKE_ROOT}/integration/config/#{config}.cfg' " +
       "--type #{type} " +
       "--debug " +
       "--tests " + args[:test_files] + " " +
       "--options-file 'integration/options.rb' " +
       "--root-keys"

    beaker += " --preserve-hosts" if preserve_hosts
    beaker += " --no-color" unless color
    beaker += " --xml" if xml

    sh beaker
  end
end
