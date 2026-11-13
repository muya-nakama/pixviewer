(async () => {
 'use strict';
 if (location.origin !== 'https://www.pixiv.net' || document.getElementById('muya-sort')) return;
 const parts = location.pathname.split('/');
 if (parts[1] !== 'tags' || !['artworks','illustrations','manga'].includes(parts[3])) {
  alert('イラスト・漫画の検索結果で利用できます。'); return;
 }
 const originalURL=location.href;
 const controller=new AbortController();
 const panel=document.createElement('section'); panel.id='muya-sort';
 Object.assign(panel.style,{position:'fixed',inset:'0',zIndex:'2147483647',background:'#fff',color:'#20242a',overflowY:'auto',padding:'16px',fontFamily:'sans-serif'});
 const heading=document.createElement('h2');heading.textContent='取得範囲内の人気順';
 const note=document.createElement('p');note.textContent='新着から最大60作品・ブックマーク数順';
 const progress=document.createElement('p');progress.textContent='検索結果を取得中…';progress.setAttribute('aria-live','polite');
 const close=document.createElement('button');close.textContent='閉じる / 取得中止';
 const grid=document.createElement('div');Object.assign(grid.style,{display:'grid',gridTemplateColumns:'repeat(2,minmax(0,1fr))',gap:'14px'});
 const previousOverflow=document.documentElement.style.overflow;
 let stopped=false;
 const cleanup=()=>{stopped=true;controller.abort();clearInterval(watcher);panel.remove();document.documentElement.style.overflow=previousOverflow;};
 const watcher=setInterval(()=>{if(location.href!==originalURL)cleanup();},300);
 close.onclick=cleanup;panel.addEventListener('muya-close',cleanup);
 document.documentElement.style.overflow='hidden';panel.append(close,heading,note,progress,grid);document.body.append(panel);
 const delay=()=>new Promise(resolve=>setTimeout(resolve,550));
 async function get(path){
  if(stopped)throw new Error('中止');
  const response=await fetch(path,{credentials:'same-origin',signal:controller.signal});
  if(!response.ok)throw new Error(response.status===429?'アクセス制限のため停止しました。時間をおいて再試行してください。':'通信エラー HTTP '+response.status);
  const payload=await response.json();
  if(payload.error || !payload.body)throw new Error('ログイン状態・閲覧権限、またはpixiv側の仕様変更を確認してください。');
  return payload.body;
 }
 const works=[]; const ids=new Set(); let examined=0; let skipped=0;
 function render(){
  grid.replaceChildren();
  works.sort((a,b)=>b.bookmarkCount-a.bookmarkCount || Number(b.id)-Number(a.id));
  for(const work of works){
   const card=document.createElement('a');card.href='/artworks/'+work.id;Object.assign(card.style,{color:'inherit',textDecoration:'none',minWidth:'0'});
   const img=document.createElement('img');
   try {const u=new URL(work.urls.small);if(u.protocol==='https:' && (u.hostname==='pximg.net'||u.hostname.endsWith('.pximg.net')))img.src=u.href;} catch(_){}
   img.alt=work.title;img.loading='lazy';Object.assign(img.style,{width:'100%',aspectRatio:'1',objectFit:'cover',borderRadius:'10px',background:'#f0f3f5'});
   const title=document.createElement('div');title.textContent=work.title;
   const author=document.createElement('small');author.textContent=work.userName+' · ♥ '+work.bookmarkCount.toLocaleString();
   card.append(img,title,author);grid.append(card);
  }
 }
 try {
  const query=new URLSearchParams(location.search);query.set('word',decodeURIComponent(parts[2]));query.set('order','date_d');query.set('lang','ja');
  // Never request premium sort/filter parameters; preserve other active search conditions.
  query.delete('blt');query.delete('bgt');
  for(let page=1;page<=2 && examined<60;page++){
   query.set('p',String(page));
   const body=await get('/ajax/search/'+parts[3]+'/'+parts[2]+'?'+query);
   const group=body.illustManga || body.illust || body.manga;
   if(!group || !Array.isArray(group.data))throw new Error('検索データ形式が変わっています。公式画面で検索を続けてください。');
   if(!group.data.length)break;
   for(const item of group.data){
    const id=String(item.id);
    if(!/^\d+$/.test(id)||ids.has(id)||examined>=60)continue;
    ids.add(id);examined++;await delay();
    const work=await get('/ajax/illust/'+id);
    if(typeof work.bookmarkCount==='number' && work.urls && work.urls.small){works.push(work);}else skipped++;
    if(stopped)return;
    progress.textContent=examined+'作品確認・'+works.length+'作品を表示'+(skipped?'（数値・画像なし '+skipped+'件）':'');
    render();
   }
   await delay();
  }
  progress.textContent='完了：'+works.length+'作品を並べ替え（確認 '+examined+'件）。検索結果全体の順位ではありません。';
 }catch(error){if(!stopped){render();progress.textContent='取得を停止：'+error.message+' 取得済み '+works.length+'作品のみ表示しています。';}}
})();
