import { defineConfig } from 'vitepress';

export default defineConfig({
  title: 'scope',
  description: 'A small JVM library that treats scopes as first-class runtime objects.',
  base: '/scope/',
  cleanUrls: true,
  themeConfig: {
    nav: [
      { text: 'Guide', link: '/mental-model' },
      { text: 'API', link: '/api-reference' },
      { text: 'Javadoc', link: '/javadoc/', target: '_blank', rel: 'noreferrer' },
      { text: 'GitHub', link: 'https://github.com/theking90000/scope' }
    ],
    sidebar: [
      {
        text: 'Guide',
        items: [
          { text: 'Mental model', link: '/mental-model' },
          { text: 'Getting started', link: '/getting-started' },
          { text: 'Injection', link: '/injection' },
          { text: 'Qualifiers & collections', link: '/qualifiers-and-collections' },
          { text: 'Scopes & lifecycle', link: '/scopes-and-lifecycle' },
          { text: 'Multi-parent scopes', link: '/multi-parent' },
          { text: 'Extension hooks', link: '/extension-hooks' },
          { text: 'API reference', link: '/api-reference' },
          { text: 'Exceptions', link: '/exceptions' },
          { text: 'Recipes', link: '/recipes' }
        ]
      }
    ],
    socialLinks: [
      { icon: 'github', link: 'https://github.com/theking90000/scope' }
    ],
    search: {
      provider: 'local'
    }
  }
});
