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


## Configuring the EMR cluster

`TODO`


## Usage

1. Copy your input data into S3 bucket
1. Process your S3 text data: `./bin/process.sh <input-filename> <tokenized-filename> <lemmatized-filename>`

Example usage (with `aws-cli` for data sync):
```bash 
# "my-input-data.txt" is a folder with hadoop data format 
aws s3 sync ./my-input-data.txt s3://my-s3-nlp-bucket/data/input.txt

./bin/process.sh s3://my-s3-nlp-bucket/data/input.txt \
  s3://my-s3-nlp-bucket/data/output/tokenized.txt \
  s3://my-s3-nlp-bucket/data/output/lemmatized.txt
```

`process.sh` creates a new *"las-emr"* named cluster that processes the given input file and
terminates automatically when the job is completed. If you want to examine the progress of
the processing job, you can do it by using [Spark Web UI](http://docs.aws.amazon.com/emr/latest/ReleaseGuide/emr-spark-history.html).


## License

GNU GPLv3

(I know and I'm sad as well but it can't be helped since some of the transient 
dependencies have that license... :cry: However, note that because this tool runs
in a separate process, you can build your other NLP tools and infra without
exposing them to the license.)

