module XcconfigHelper
  def xcconfig_paths
    [
      File.expand_path("../Configuration/Config.xcconfig", __dir__),
      File.expand_path("../Configuration/Signing.local.xcconfig", __dir__)
    ]
  end

  def xcconfig
    return @xcconfig if defined?(@xcconfig)

    @xcconfig = {}

    xcconfig_paths.each do |path|
      next unless File.exist?(path)

      File.foreach(path) do |line|
        stripped = line.sub(%r{//.*$}, "").strip
        next if stripped.empty? || stripped.start_with?("#")

        key, value = stripped.split("=", 2).map(&:strip)
        @xcconfig[key] = value if key && value
      end
    end

    @xcconfig
  end

  def xcconfig_value(key)
    ENV[key] || xcconfig[key]
  end
end
