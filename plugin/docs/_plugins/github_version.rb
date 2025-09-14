# Jekyll plugin to fetch latest version from GitHub releases
require 'net/http'
require 'json'
require 'uri'

module Jekyll
  class GitHubVersionTag < Liquid::Tag
    def initialize(tag_name, text, tokens)
      super
      @repo = text.strip
    end

    def render(context)
      return @cached_version if @cached_version

      begin
        repo = @repo.empty? ? context.site.data['plugin']['repository'] : @repo
        repo ||= context.site.config['repository']
        
        return "2.1.0" unless repo

        uri = URI("https://api.github.com/repos/#{repo}/releases/latest")
        response = Net::HTTP.get_response(uri)
        
        if response.code == '200'
          data = JSON.parse(response.body)
          @cached_version = data['tag_name'].gsub(/^v/, '')
        else
          @cached_version = context.site.config.dig('version', 'current') || "2.1.0"
        end
      rescue => e
        Jekyll.logger.warn("GitHub Version", "Could not fetch version: #{e.message}")
        @cached_version = context.site.config.dig('version', 'current') || "2.1.0"
      end

      @cached_version
    end
  end

  class GitHubVersionGenerator < Generator
    priority :highest
    
    def generate(site)
      return unless site.config.dig('version', 'fetch_from_github')
      
      begin
        repo = site.config['repository']
        return unless repo
        
        uri = URI("https://api.github.com/repos/#{repo}/releases/latest")
        response = Net::HTTP.get_response(uri)
        
        if response.code == '200'
          data = JSON.parse(response.body)
          version = data['tag_name'].gsub(/^v/, '')
          
          # Make version available globally
          site.data['version'] = {
            'current' => version,
            'tag' => data['tag_name'],
            'published_at' => data['published_at'],
            'url' => data['html_url']
          }
          
          Jekyll.logger.info("GitHub Version", "Fetched version #{version} from GitHub")
        end
      rescue => e
        Jekyll.logger.warn("GitHub Version", "Could not fetch version: #{e.message}")
      end
    end
  end
end

Liquid::Template.register_tag('github_version', Jekyll::GitHubVersionTag)