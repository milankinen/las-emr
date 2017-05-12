# LAS EMR

Parallelized Finnish text pre-processing with AWS EMR and 
[SeCo Lexical Analysis Services](https://github.com/jiemakel/seco-lexicalanalysis).

## Motivation

Some text pre-processing tasks, such as proper tokenization and lemmatization, are
computationally challenging. There already exist many good tools that produce proper 
output, but take long time to run: pre-processing tens of gigabytes of text might 
take days, even weeks. Luckily some of these tools are stateless, meaning that they
can be parallelized easily.

This repository contains a text pre-processor utility for Finnish NLP tasks. 
It wraps [SeCo Lexical Analysis Services](https://github.com/jiemakel/seco-lexicalanalysis) 
as an [Apache Spark](http://spark.apache.org) job that can process raw Finnish
text files from AWS S3 with AWS [EMR](https://aws.amazon.com/emr/) cluster and output
the processed (tokenized and lemmatized) files back into S3. The process is entirely 
stateless so you can increase the cluster size in order to boost the pre-processing.


## Setup

1. Make sure you have `java8-jdk`, `maven` installed
1. Obtain your AWS credentials from AWS console and configure your shell to use them
1. Create a new S3 bucket that will contain the nlp files (input data, output data 
and binaries)
1. Build the binaries: `./bin/build.sh`
1. Deploy the binaries: `./bin/deploy.sh <your-bucket-name>`


## Usage

1. Copy your input data into S3 bucket
1. Process your S3 text data with `bin/process.sh` script (use `--help` for more info)

Example usage (with `aws-cli` for data sync):
```bash 
# "my-input-data.txt" is a folder with hadoop data format 
aws s3 sync ./my-input-data.txt s3://my-s3-nlp-bucket/data/input.txt

# create default roles if you've not used emr before
aws emr create-default-roles

./bin/process.sh \
  --bucket my-finnlp-bucket \
  --input s3n://my-finnlp-bucket/data/input/mydata.txt \
  --tokens s3n://my-finnlp-bucket/data/output/tokenized.txt \
  --lemmas s3n://my-finnlp-bucket/data/output/lemmatized.txt
```

`process.sh` creates a new *"las-emr"* named cluster that processes the given input file and
terminates automatically when the job is completed. If you want to examine the progress of
the processing job, you can do it by using [Spark Web UI](http://docs.aws.amazon.com/emr/latest/ReleaseGuide/emr-spark-history.html).


## Custom EMR flow configurations

The default configuration uses `m1.large` instances for `CORE` nodes because they
don't have any special requirements for the cluster. However, `m1.*` instances are
pretty slow (single `m1.large` instance can process approximately 2.3 MB Finnish text
per hour) so it's recommended to use `c4.*` instances instead.

You can customize the flow and cluster configurations by using `--config my_config.edn`
flag. The contents of your configuration file will be merged to `defaults.edn` configurations.
For full configuration options, please see [amazonica](https://github.com/mcohen01/amazonica)
documentation.

**ATTENTION!** When using custom instance types, remember to set executor memory and core settings 
to utilize the maximum resources of your instances. Your instances should have big enough so that
each executors get at least `3G` memory. The EMR specs for different instance types can be found 
[here](http://docs.aws.amazon.com/emr/latest/ReleaseGuide/emr-hadoop-task-config.html).

Here is an example configuration with custom cluster name and `c4.xlarge` core nodes:
```clojure
; my_custom_cluster.edn
{:name           "big-spot-las"
 :configurations [{:classification "spark"
                    :properties     {"maximizeResourceAllocation" "true"}}
                   {:classification "spark-defaults"
                    :properties     {"spark.executor.memory" "5120m"
                                     "spark.executor.cores"  "4"}}]
 :instances       {:ec2-subnet-id 
                   "subnet-abc12345"    ; required for c4 instances
                   :instance-groups
                   [{:instance-type  "m1.medium"
                     :instance-role  "MASTER"
                     :instance-count 1}
                    {:instance-type  "c4.xlarge"
                     :instance-role  "CORE"
                     :instance-count 100
                     :market         "SPOT"
                     :bid-price      "0.06"}]}}
```
And starting the job:
```bash
./bin/process.sh \
  --conf my_custom_cluster.edn \
  --bucket my-finnlp-bucket \
  --input s3n://my-finnlp-bucket/data/input/bigdata.txt \
  --tokens s3n://my-finnlp-bucket/data/output/tokenized.txt \
  --lemmas s3n://my-finnlp-bucket/data/output/lemmatized.txt
```

## Some thoughts about time and costs

One `c4.xlarge` instance (with the configurations shown as above) can process Finnish
text approximately **6.2 MB/hour** which means that with a cluster of 100 instances, 
you can process approximately *10 GB* of data in *16,2 hours*. If you're using spot 
market with e.g. *$0.06/h* price cap, the cost for this job will be approximately __$185__. 

## License

GNU GPLv3

(I know and I'm sad as well but it can't be helped since some of the transient 
dependencies have that license... :cry: However, note that because this tool runs
in a separate process, you can build your other NLP tools and infra without
exposing them to the license.)

