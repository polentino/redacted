import type {ReactNode} from 'react';
import clsx from 'clsx';
import Heading from '@theme/Heading';
import styles from './styles.module.css';

type FeatureItem = {
    title: string;
    Svg: React.ComponentType<React.ComponentProps<'svg'>>;
    description: ReactNode;
};

const FeatureList: FeatureItem[] = [
    {
        title: 'Easy to Use',
        Svg: require('@site/static/img/redacted_logo_no_bg_easy.svg').default,
        description: (
            <>
                <code>@redacted</code> is designed to be easy to integrate and use in your current project: in three easy
                steps you will be all set up!
            </>
        ),
    },
    {
        title: 'Focus on What Matters',
        Svg: require('@site/static/img/redacted_logo_no_bg_focus.svg').default,
        description: (
            <>
                Mark sensitive fields with <code>@redacted</code> and fear no more of mistakenly leak sensitive/PII
                infos in console, log, or as http/gRPC error responses!
            </>
        ),
    },
    {
        title: 'Cross-Platform Support',
        Svg: require('@site/static/img/redacted_logo_no_bg_scala.svg').default,
        description: (
            <>
                <code>@redacted</code> works seamlessly across <b>JVM</b>, <b>Scala.js</b>, and <b>Scala Native</b>,
                supporting both Scala <code>3.x</code> and <code>2.x</code>.
            </>
        ),
    },
];

function Feature({title, Svg, description}: FeatureItem) {
    return (
        <div className={clsx('col col--4')}>
            <div className="text--center">
                <Svg className={styles.featureSvg} role="img"/>
            </div>
            <div className="text--center padding-horiz--md">
                <Heading as="h3">{title}</Heading>
                <p>{description}</p>
            </div>
        </div>
    );
}

export default function HomepageFeatures(): ReactNode {
    return (
        <section className={styles.features}>
            <div className="container">
                <div className="row">
                    {FeatureList.map((props, idx) => (
                        <Feature key={idx} {...props} />
                    ))}
                </div>
            </div>
        </section>
    );
}
