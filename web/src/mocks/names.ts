export const FIRST_NAMES = [
  'Denis', 'Allan', 'Patience', 'Jordan', 'Mariam', 'Godfrey', 'Ismael', 'Kevin',
  'Grace', 'Brenda', 'Ronald', 'Sarah', 'David', 'Esther', 'Peter', 'Joan',
  'Moses', 'Josephine', 'Robert', 'Diana', 'Emmanuel', 'Sandra', 'Isaac', 'Winnie',
  'Joseph', 'Florence', 'Samuel', 'Agnes', 'Daniel', 'Prossy', 'Andrew', 'Immaculate',
  'Richard', 'Harriet', 'Vincent', 'Rebecca', 'Herbert', 'Sheilah', 'Bruno', 'Doreen',
]

export const LAST_NAMES = [
  'Mugisha', 'Nakato', 'Okello', 'Atim', 'Ssekandi', 'Namutebi', 'Byaruhanga', 'Auma',
  'Tumusiime', 'Nabirye', 'Kwikiriza', 'Achola', 'Kato', 'Nansubuga', 'Wamala', 'Adong',
  'Kirabo', 'Odongo', 'Nakawesi', 'Bwambale', 'Asiimwe', 'Nalubega', 'Tugume', 'Aceng',
  'Businge', 'Nantongo', 'Kyeyune', 'Apio', 'Twinomujuni', 'Nabatanzi',
]

export const DISTRICTS = ['Mbarara', 'Kampala', 'Ntungamo', 'Bushenyi', 'Ibanda', 'Isingiro', 'Kabale', 'Sheema']

export function fullName(rng: () => number, first: readonly string[], last: readonly string[]): string {
  const f = first[Math.floor(rng() * first.length)]
  const l = last[Math.floor(rng() * last.length)]
  return `${f} ${l}`
}
