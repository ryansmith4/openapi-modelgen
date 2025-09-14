# Dynamic Version System for Documentation

This project uses a dynamic version system that automatically keeps documentation up-to-date with the latest plugin releases.

## How It Works

### 1. Jekyll Data Files
The version is stored in `_data/plugin.yml` and automatically updated by GitHub Actions when a new release is published.

### 2. Multiple Version Sources (Priority Order)
1. **`site.data.version.current`** - Updated by GitHub Actions from latest release
2. **`site.data.plugin.current_version`** - Fallback in plugin data file
3. **`site.version.current`** - Fallback in Jekyll config
4. **`"2.1.0"`** - Hard-coded fallback

### 3. Usage in Documentation

#### Simple Include (Recommended)
```liquid
id 'com.guidedbyte.openapi-modelgen' version '{% include plugin_version.html %}'
```

#### Direct Liquid Variable
```liquid
{{ site.data.version.current | default: site.data.plugin.current_version | default: "2.1.0" }}
```

#### With GitHub Plugin (Build-time Fetch)
```liquid
{% github_version %}
```

#### Client-side JavaScript (Real-time)
```html
<span class="plugin-version">2.1.0</span>
```
The JavaScript in `_includes/latest_version.html` will update these automatically.

## Implementation Examples

### Example 1: Plugin Declaration
```gradle
plugins {
    id 'org.openapi.generator' version '7.14.0'
    id 'com.guidedbyte.openapi-modelgen' version '{% include plugin_version.html %}'
}
```

### Example 2: Dependency Declaration  
```gradle
dependencies {
    openapiCustomizations 'com.company:api-templates:{% include plugin_version.html %}'
}
```

### Example 3: Version Display
```markdown
Current plugin version: **{% include plugin_version.html %}**
```

## Automatic Updates

### GitHub Actions Workflow
The `update-docs-version.yml` workflow automatically:
1. Triggers on new releases
2. Fetches the latest version from GitHub Releases API
3. Updates `_data/plugin.yml` with the new version
4. Commits and pushes the changes
5. Triggers GitHub Pages rebuild

### Manual Update
You can manually trigger version updates:
```bash
# Trigger the workflow manually
gh workflow run update-docs-version.yml
```

## Fallback Strategy

The system is designed to be robust with multiple fallbacks:

1. **GitHub Actions fails**: Falls back to data file version
2. **Data file missing**: Falls back to Jekyll config version  
3. **All dynamic sources fail**: Falls back to hard-coded version
4. **Client-side fetch fails**: Falls back to server-side rendered version

## Files Involved

- **`_data/plugin.yml`** - Main version data store
- **`_config.yml`** - Jekyll configuration with fallback version
- **`_includes/plugin_version.html`** - Simple version include
- **`_includes/latest_version.html`** - JavaScript version fetcher
- **`_plugins/github_version.rb`** - Server-side GitHub API plugin
- **`.github/workflows/update-docs-version.yml`** - Automation workflow

## Best Practices

1. **Always use includes**: `{% include plugin_version.html %}` instead of direct variables
2. **Test locally**: Jekyll will use fallback versions during local development
3. **Monitor automation**: Check that GitHub Actions workflow completes successfully
4. **Validate updates**: Verify documentation renders correctly after version updates

## Troubleshooting

### Version Not Updating
1. Check GitHub Actions workflow logs
2. Verify `_data/plugin.yml` was updated
3. Ensure GitHub Pages rebuild completed
4. Clear browser cache

### Local Development
During local development with `jekyll serve`, the system will use fallback versions from the config file since GitHub API may not be accessible.

### GitHub API Rate Limits
The client-side JavaScript approach may hit rate limits. The server-side approaches (GitHub Actions + Jekyll plugins) are more reliable for production.