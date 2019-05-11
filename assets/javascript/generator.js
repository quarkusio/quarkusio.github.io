function generate() {
  var groupId = $('#form').find('input[name="groupId"]').val();
  var artifactId = $('#form').find('input[name="artifactId"]').val();
  var version = $('#form').find('input[name="version"]').val();
  var className = $('#form').find('input[name="className"]').val();

  if (! /\S/.test(groupId)) {
    groupId = "org.acme.quarkus.sample";
  }
  if (! /\S/.test(artifactId)) {
    artifactId = "my-quarkus-project";
  }
  if (! /\S/.test(version)) {
    version = "1.0-SNAPSHOT";
  }
  if (! /\S/.test(className)) {
    className = "org.acme.quarkus.sample.HelloResource";
  }

  var extensions = [];
  $.each($("#form .extensions option:selected"), function(){            
      extensions.push($(this).val());
  });

  var downloadPath = 'http://localhost:8080/generator?g='+groupId+'&a='+artifactId+'&pv='+version+'&cn='+className+'&e='+extensions.map(function(i){return i;}).join('&e=');
  window.location = downloadPath;
  return false;
}
