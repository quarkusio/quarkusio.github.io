FROM ruby:alpine

RUN apk add --no-cache \
    bash \
    build-base \
    cmake \
    curl \
    gcc \
    git \
    outils-md5

RUN gem install bundler
COPY Gemfile Gemfile.lock ./
RUN bundle install

#CMD [ "bundle", "exec", "jekyll", "serve", "--livereload", "-H", "0.0.0.0", "-P", "4000" ]

EXPOSE 4000 35729