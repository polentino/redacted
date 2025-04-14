import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

// This runs in Node.js - Don't use client-side code here (browser APIs, JSX...)

const config: Config = {
  title: 'Redacted',
  tagline: 'Scala annotation & compiler plugin to prevent leaking sensitive / PII fields defined inside case class.',
  favicon: 'img/favicon.ico',

  // Set the production url of your site here
  url: 'https://your-docusaurus-site.example.com',
  // Set the /<baseUrl>/ pathname under which your site is served
  // For GitHub pages deployment, it is often '/<projectName>/'
  baseUrl: '/',

  // GitHub pages deployment config.
  // If you aren't using GitHub pages, you don't need these.
  // organizationName: 'facebook', // Usually your GitHub org/user name. // TODO REMOVE
  // projectName: 'docusaurus', // Usually your repo name. // TODO REMOVE
  organizationName: 'io.github.polentino', // Usually your GitHub org/user name.
  projectName: 'redacted', // Usually your repo name.

  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',

  // Even if you don't use internationalization, you can use this field to set
  // useful metadata like html lang. For example, if your site is Chinese, you
  // may want to replace "en" with "zh-Hans".
  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      {
        docs: {
          sidebarPath: './sidebars.ts',
        },
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],

  themeConfig: {
    // Replace with your project's social card
    image: 'img/docusaurus-social-card.jpg',
    navbar: {
      title: 'Redacted',
      logo: {
        alt: 'My Site Logo',
        src: 'img/redacted_logo.svg',
      },
      items: [
        {
          type: 'docSidebar',
          sidebarId: 'tutorialSidebar',
          position: 'left',
          label: 'Getting Started',
        },
        {
          position: 'left',
          label: "Motivation",
          to: "/motivation",
        },
        {
          position: 'left',
          label: "Development",
          to: "/development",
        },
        {
          href: 'https://github.com/polentino/redacted',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: 'Documentation',
          items: [
            {
              label: 'Getting Started',
                to: '/docs/category/installation',
            },
            {
              label: 'Motivation',
                to: '/motivation',
            },
            {
              label: 'Development',
                to: '/development',
            },
          ],
        },
        {
          title: 'Developers',
          items: [
            {
              label: '@polentino911 (Twitter)',
              href: 'https://twitter.com/polentino911',
            },
          ],
        },
        {
          title: 'More',
          items: [
            {
              label: 'GitHub',
              href: 'https://github.com/polentino/redacted',
            },
          ],
        },
      ],
      copyright: `<code>@edacted</code> copyright ©${new Date().getFullYear()} by Diego Casella. Made with Docusaurus.`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.duotoneDark,
      additionalLanguages: ['clike', 'java', 'scala'],
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
