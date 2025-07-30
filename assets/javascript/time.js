 // Option 1: Get from a data attribute
 const utcDateString = document.getElementById('convertedTime').dataset.utc;

 // Convert to user's local date and time
 const localDate = new Date(utcDateString);

 // Format the output
 const options = {
   weekday: 'long',
   year: 'numeric',
   month: 'long',
   day: 'numeric',
   hour: '2-digit',
   minute: '2-digit',
   timeZoneName: 'short'
 };

 const localDateTimeString = localDate.toLocaleString(undefined, options);

 // Set the readable local date as content
 document.getElementById("convertedTime").textContent = `Date & Time: ${localDateTimeString}`;